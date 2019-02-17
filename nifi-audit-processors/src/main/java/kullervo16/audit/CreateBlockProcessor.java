/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package kullervo16.audit;

import kullervo16.audit.utils.CryptoUtils;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.nifi.annotation.behavior.Stateful;
import org.apache.nifi.annotation.documentation.CapabilityDescription;
import org.apache.nifi.annotation.documentation.Tags;
import org.apache.nifi.annotation.lifecycle.OnScheduled;
import org.apache.nifi.components.PropertyDescriptor;
import org.apache.nifi.components.state.Scope;
import org.apache.nifi.flowfile.FlowFile;
import org.apache.nifi.flowfile.attributes.CoreAttributes;
import org.apache.nifi.processor.*;
import org.apache.nifi.processor.exception.ProcessException;
import org.apache.nifi.processor.util.StandardValidators;

import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.security.KeyFactory;
import java.security.PrivateKey;
import java.security.spec.PKCS8EncodedKeySpec;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.zip.GZIPOutputStream;

@Tags({"audit","blockchain","couchdb"})
@Stateful(scopes = Scope.CLUSTER, description = "Stores the block number and last hash/execution time")
@CapabilityDescription("Converts couchDB event stream(s) into audit blocks. It takes both a size and a duration, whichever is reached first will trigger the creation of a block")
public class CreateBlockProcessor extends AbstractProcessor {
    public static final String INTERVAL = "interval";
    public static final PropertyDescriptor PROPERTY_INTERVAL = new PropertyDescriptor.Builder()
            .name(INTERVAL)
            .description("interval between blocks in minutes (default = 15)")
            .defaultValue("15")
            .addValidator(StandardValidators.POSITIVE_INTEGER_VALIDATOR)
            .build();
    public static final String BLOCKSIZE = "blocksize";
    public static final PropertyDescriptor PROPERTY_BLOCKSIZE = new PropertyDescriptor.Builder()
            .name(BLOCKSIZE)
            .description("maximum number of elements per block (default = 1000)")
            .required(false)
            .defaultValue("1000")
            .addValidator(StandardValidators.POSITIVE_INTEGER_VALIDATOR)
            .build();

    public static final String PRIVATE_KEY = "private key";
    public static final PropertyDescriptor PROPERTY_PRIVATE_KEY = new PropertyDescriptor.Builder()
            .name(PRIVATE_KEY)
            .description("base64 form of the private key to sign with")
            .required(true)
            .sensitive(true)
            .addValidator(StandardValidators.NON_EMPTY_VALIDATOR)
            .build();



    public static final Relationship SUCCESS_REL = new Relationship.Builder()
            .name("block")
            .build();
    public static final String LAST_EXECUTION = "lastExecution";
    public static final String BLOCK_NUMBER = "blockNumber";
    public static final String LAST_HASH = "lastHash";
    private static final String SIGNATURE = "signature";


    private List<PropertyDescriptor> descriptors;

    private Set<Relationship> relationships;

    private PrivateKey pk;
    private String pkFingerprint;




    @Override
    protected void init(final ProcessorInitializationContext context) {
        final List<PropertyDescriptor> descriptors = new ArrayList<PropertyDescriptor>();
        descriptors.add(PROPERTY_BLOCKSIZE);
        descriptors.add(PROPERTY_INTERVAL);
        descriptors.add(PROPERTY_PRIVATE_KEY);
        this.descriptors = Collections.unmodifiableList(descriptors);

        final Set<Relationship> relationships = new HashSet<Relationship>();
        relationships.add(SUCCESS_REL);
        this.relationships = Collections.unmodifiableSet(relationships);

    }

    @Override
    public Set<Relationship> getRelationships() {
        return this.relationships;
    }

    @Override
    public final List<PropertyDescriptor> getSupportedPropertyDescriptors() {
        return descriptors;
    }

    @OnScheduled
    public void onScheduled(final ProcessContext context) {

    }

    @Override
    public void onPropertyModified(PropertyDescriptor descriptor, String oldValue, String newValue) {
        super.onPropertyModified(descriptor, oldValue, newValue);
        if(descriptor.getName().equals(PRIVATE_KEY)) {

            try {
                byte[] b1 = Base64.getDecoder().decode(newValue);
                PKCS8EncodedKeySpec spec = new PKCS8EncodedKeySpec(b1);
                KeyFactory kf = KeyFactory.getInstance("RSA");
                this.pk = kf.generatePrivate(spec);
                this.pkFingerprint = newValue.substring(newValue.length()-5);
            } catch (Exception e) {
                getLogger().error("Invalid private key");
            }
        }
    }

    @Override
    public void onTrigger(final ProcessContext context, final ProcessSession session) throws ProcessException {

        State state = loadState(context);
        if(!conditionsMet(context, session, state.getLastExecution())) {
            context.yield();
            return;
        }

        FlowFile incomingFlowFile = session.get();
        if ( incomingFlowFile == null ) {
            // no data, so just update the execution time
            state.setLastExecution(System.currentTimeMillis());
            this.saveState(context, state);
            return;
        }
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        FlowFile outgoingFlowFile = session.create();
        try(OutputStream os = session.write(outgoingFlowFile);
            GZIPOutputStream gzos = new GZIPOutputStream(os);
            PrintWriter writer = new PrintWriter(new OutputStreamWriter(gzos))) {
            StringBuilder blockContents = new StringBuilder("Hash of previous block: ");
            blockContents.append(state.getLastHash()).append("\n");
            while (incomingFlowFile != null) {

                long time = incomingFlowFile.getLineageStartDate();
                blockContents.append(""+time).append("|");
                blockContents.append(sdf.format(new Date(time))).append("|");
                blockContents.append(incomingFlowFile.getAttribute("event.source")).append("|");
                blockContents.append(incomingFlowFile.getAttribute("doc.id")).append("|");
                blockContents.append(incomingFlowFile.getAttribute("doc.rev")).append("\n");

                session.remove(incomingFlowFile);
                incomingFlowFile = session.get();
            }
            long blockDone = System.currentTimeMillis();
            blockContents.append("Block created at: ").append(""+blockDone).append("|").append(sdf.format(new Date(blockDone))).append("\n");
            String content = blockContents.toString();
            writer.write(content);

            // hash of previous block is part of it... this constructs the chain
            state.setLastHash(DigestUtils.sha512Hex(content));
            state.setSignature(CryptoUtils.signValue(this.pk, content));
            writer.write("sha512: ");
            writer.write(state.getLastHash());
            writer.write("\n");
            writer.write("SHA512withRSA: ");
            writer.write(state.getSignature());
            writer.write("\n");
            writer.write("fingerprint: ");
            writer.write(this.pkFingerprint);
            writer.write("\n");
            writer.flush();
            writer.close();
            gzos.close();
            os.close();
            state.setBlockNumber(state.getBlockNumber()+1);
            outgoingFlowFile = session.putAttribute(outgoingFlowFile, CoreAttributes.FILENAME.key(), "block_"+state.getBlockNumber());
            outgoingFlowFile = session.putAttribute(outgoingFlowFile, CoreAttributes.MIME_TYPE.key(), "application/gzip");

            session.transfer(outgoingFlowFile, SUCCESS_REL);
            state.setLastExecution(System.currentTimeMillis());
            saveState(context, state);
        }catch(Exception ioe) {
            getLogger().error(ioe.getMessage(), ioe);
            session.rollback();
        }
    }

    private State loadState(ProcessContext context) {

        State state = new State();
        try {
            Map<String, String> stateMap = context.getStateManager().getState(Scope.CLUSTER).toMap();
            if(stateMap != null) {
                if (stateMap.containsKey(LAST_EXECUTION)) {
                    state.setLastExecution(Long.parseLong(stateMap.get(LAST_EXECUTION)));
                }
                if (stateMap.containsKey(BLOCK_NUMBER)) {
                    state.setBlockNumber(Integer.valueOf(stateMap.get(BLOCK_NUMBER)));
                }
                if (stateMap.containsKey(LAST_HASH)) {
                    state.setLastHash(stateMap.get(LAST_HASH));
                }
                if(stateMap.containsKey(SIGNATURE)) {
                    state.setSignature(stateMap.get(SIGNATURE));
                }
            }

        } catch (IOException e) {
            getLogger().error("Could not get state",e);
            throw new ProcessException(e);
        }
        return state;
    }

    private void saveState(ProcessContext context,State state) {


        try {
            Map<String, String> stateMap = context.getStateManager().getState(Scope.CLUSTER).toMap();
            if(stateMap == null) {
                stateMap = new HashMap<>();
                // TODO : use data in blocks to initialize the data (allows major nifi crash as well)
            } else {
                stateMap = new HashMap<>(stateMap);
            }

            stateMap.put(LAST_EXECUTION, ""+state.getLastExecution());
            stateMap.put(LAST_HASH, state.getLastHash());
            stateMap.put(BLOCK_NUMBER, ""+state.getBlockNumber());
            stateMap.put(SIGNATURE, state.getSignature());
            context.getStateManager().setState(stateMap, Scope.CLUSTER);

        } catch (IOException e) {
            getLogger().error("Could not get state",e);
            throw new ProcessException(e);
        }

    }

    /**
     * We execute when either the number of waiting elements is larger than the requested blocksize or when the last
     * execution was longer ago than the required interval (in which case we may create a partial block)
     * @param context
     * @param session
     * @param lastExecution
     * @return
     */
    private boolean conditionsMet(ProcessContext context, ProcessSession session, long lastExecution) {
        return session.getQueueSize().getObjectCount() > context.getProperty(BLOCKSIZE).asInteger() ||
                (System.currentTimeMillis() - lastExecution) > context.getProperty(INTERVAL).asInteger() * 1000 * 60;
    }
}

class State {
    private long lastExecution = 0;
    private int blockNumber = 0;
    private String lastHash = "root";
    private String signature = "";

    public long getLastExecution() {
        return lastExecution;
    }

    public void setLastExecution(long lastExecution) {
        this.lastExecution = lastExecution;
    }

    public int getBlockNumber() {
        return blockNumber;
    }

    public void setBlockNumber(int blockNumber) {
        this.blockNumber = blockNumber;
    }

    public String getLastHash() {
        return lastHash;
    }

    public void setLastHash(String lastHash) {
        this.lastHash = lastHash;
    }

    public String getSignature() {
        return signature;
    }

    public void setSignature(String signature) {
        this.signature = signature;
    }
}
