import com.google.common.io.BaseEncoding;
import com.google.protobuf.ByteString;

import co.nstant.in.cbor.CborDecoder;
import co.nstant.in.cbor.CborException;
import co.nstant.in.cbor.model.DataItem;
import org.json.JSONException;
import org.json.JSONObject;
import sawtooth.sdk.processor.State;
import sawtooth.sdk.processor.TransactionHandler;
import sawtooth.sdk.processor.Utils;
import sawtooth.sdk.processor.exceptions.InternalError;
import sawtooth.sdk.processor.exceptions.InvalidTransactionException;
import sawtooth.sdk.protobuf.*;

import java.io.ByteArrayInputStream;
import java.io.UnsupportedEncodingException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.*;
import java.util.logging.Logger;

public class CovidTrackerHandler implements TransactionHandler {

    private final Logger logger = Logger.getLogger(CovidTrackerHandler.class.getName());
    private final static String version = "1.0";
    private final static String txnFamilyName = "covidTracker";
    private String headerPublicKey;
    private String simpleWalletNameSpace;

    CovidTrackerHandler() {
        try {
            simpleWalletNameSpace = Utils.hash512(txnFamilyName.getBytes("UTF-8")).substring(0, 6);
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void apply(TpProcessRequest transactionRequest, State state) throws InvalidTransactionException, InternalError {
        try {
            String action = new JSONObject(decodeData(transactionRequest).toString()).getString("ACTION");

            if(action.equalsIgnoreCase("publish_positive")){
                headerPublicKey = transactionRequest.getHeader().getSignerPublicKey();

                String submittedData = new JSONObject(decodeData(transactionRequest).toString()).getString("KEY");

                submitRecord(state, submittedData, headerPublicKey);
            }

            if(action.equalsIgnoreCase("publish_location")){
                headerPublicKey = transactionRequest.getHeader().getSignerPublicKey();

                String submittedData = new JSONObject(decodeData(transactionRequest).toString()).getString("KEY");

                submitLocationRecord(state, submittedData, headerPublicKey);
            }
            
            // Test to make up to date node ips available for all new devices
            //submitIPRecord(state, getKnownIpAddress().get(0), getAddressForIPs());

        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    private void submitRecord(State stateInfo, String submittedData, String userKey) throws InvalidTransactionException, InternalError {
        // Get the wallet key derived from the wallet user's public key
        String walletKey = getKey(userKey, "user");

        ByteString bytes = ByteString.copyFromUtf8(submittedData);
        Map.Entry<String, ByteString> entry = new AbstractMap.SimpleEntry<>(walletKey, bytes);

        Collection<Map.Entry<String, ByteString>> newLedgerEntry = Collections.singletonList(entry);

        stateInfo.setState(newLedgerEntry);
    }

    private void submitLocationRecord(State stateInfo, String submittedData, String userKey) throws InvalidTransactionException, InternalError {
        // Get the wallet key derived from the wallet user's public key
        String walletLocationKey = getKey(userKey, "location");

        ByteString bytes = ByteString.copyFromUtf8(submittedData);
        Map.Entry<String, ByteString> entry = new AbstractMap.SimpleEntry<>(walletLocationKey, bytes);

        Collection<Map.Entry<String, ByteString>> newLedgerEntry = Collections.singletonList(entry);

        stateInfo.setState(newLedgerEntry);
    }

    private void submitIPRecord(State stateInfo, String submittedData, String address) throws InvalidTransactionException, InternalError {
        ByteString bytes = ByteString.copyFromUtf8(submittedData);
        Map.Entry<String, ByteString> entry = new AbstractMap.SimpleEntry<>(address, bytes);

        Collection<Map.Entry<String, ByteString>> newLedgerEntry = Collections.singletonList(entry);

        stateInfo.setState(newLedgerEntry);
    }

    private DataItem decodeData(TpProcessRequest transactionRequest) {
        ByteArrayInputStream bais = new ByteArrayInputStream(transactionRequest.getPayload().toByteArray());
        DataItem dataItems = null;
        try {
            dataItems = new CborDecoder(bais).decodeNext();
        } catch (CborException e) {
            e.printStackTrace();
        }
        return dataItems;
    }

    private String getKey(String userKey, String keyType) {
        try {
            switch (keyType) {
                case "user":
                    return hashDigestByteArrayOutputStream(txnFamilyName.getBytes()).substring(0, 6) + hashDigestByteArrayOutputStream(userKey.getBytes()).substring(0, 64);
                case "location":
                    return hashDigestByteArrayOutputStream((txnFamilyName + "location").getBytes()).substring(0, 6) + hashDigestByteArrayOutputStream(userKey.getBytes()).substring(0, 64);
                default:
                    return hashDigestByteArrayOutputStream(txnFamilyName.getBytes()).substring(0, 6) + hashDigestByteArrayOutputStream(userKey.getBytes()).substring(0, 64);
            }

        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        }
        return null;
    }

    public String hashDigestByteArrayOutputStream(byte[] input) throws NoSuchAlgorithmException {
        MessageDigest digest = MessageDigest.getInstance("SHA-512");
        digest.reset();
        digest.update(input);
        return BaseEncoding.base16().lowerCase().encode(digest.digest());
    }

    @Override
    public String transactionFamilyName() {
        return txnFamilyName;
    }

    @Override
    public String getVersion() {
        return version;
    }

    @Override
    public Collection<String> getNameSpaces() {
        ArrayList<String> namespaces = new ArrayList<>();
        namespaces.add(simpleWalletNameSpace);
        return namespaces;
    }

    private String getAddressForIPs(){
        try {
            return hashDigestByteArrayOutputStream(txnFamilyName.getBytes()).substring(0, 6) + hashDigestByteArrayOutputStream("address".getBytes()).substring(0, 64);
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        }
        return null;
    }

    private List<String> getKnownIpAddress(){
        ArrayList<String> knownIPs = new ArrayList<>();
        // dummy ips for reference
        knownIPs.add("203.129.99.232");
        return knownIPs;
    }
}