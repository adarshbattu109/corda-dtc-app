package com.capgemini.dtc.app.api;

import static java.util.Collections.singletonMap;
import static java.util.stream.Collectors.toList;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;

import javax.ws.rs.GET;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import net.corda.core.contracts.ContractState;
import net.corda.core.contracts.StateAndRef;
import net.corda.core.crypto.Party;
import net.corda.core.messaging.CordaRPCOps;

import com.capgemini.dtc.app.contract.PurchaseOrderContract;
import com.capgemini.dtc.app.flow.DTCFlow;
import com.capgemini.dtc.app.model.PurchaseOrderNew;
import com.capgemini.dtc.app.model.User;
import com.capgemini.dtc.app.state.PurchaseOrderState;
import com.capgemini.dtc.app.util.DatabaseUtil;
import com.capgemini.dtc.app.util.StringUtil;

// This API is accessible from /api/dtc. All paths specified below are relative to it.
@Path("dtc")
public class DTCApi {
    private final CordaRPCOps services;
    private final String myLegalName;

    public DTCApi(CordaRPCOps services) {
        this.services = services;
        this.myLegalName = services.nodeIdentity().getLegalIdentity().getName();
    }

    /**
     * Returns the party name of the node providing this end-point.
     */
    @GET
    @Path("me")
    @Produces(MediaType.APPLICATION_JSON)
    public Map<String, String> whoami() { return singletonMap("me", StringUtil.splitByUpperCase(myLegalName)); }

    /**
     * Returns all parties registered with the [NetworkMapService]. The names can be used to look up identities by
     * using the [IdentityService].
     */
    @GET
    @Path("peers")
    @Produces(MediaType.APPLICATION_JSON)
    public Map<String, List<String>> getPeers() {
        final String NOTARY_NAME = "Controller";
        return singletonMap(
                "peers",
                services.networkMapUpdates().getFirst()
                        .stream()
                        .map(node -> node.getLegalIdentity().getName())
                        .filter(name -> !name.equals(myLegalName) && !name.equals(NOTARY_NAME))
                        .collect(toList()));
    }

    /**
     * Return all purchase order specific to a 'userId'.
     */
    @GET
    @Path("{userId}/purchase-orders")
    @Produces(MediaType.APPLICATION_JSON)
    public List<PurchaseOrderNew> getPurchaseOrders(@PathParam("userId") String userId) {

    	List<PurchaseOrderNew> returnRecords = new ArrayList<PurchaseOrderNew>();
    	
    	List<StateAndRef<ContractState>> allRecords = services.vaultAndUpdates().getFirst();
    	
    	for(int i=0; i<allRecords.size();i++){
    		
    		StateAndRef<ContractState> singleRecord = (StateAndRef<ContractState>) allRecords.get(i);
    		
    		PurchaseOrderState state = (PurchaseOrderState) singleRecord.getState().getData();
    		
    		if(state.getPurchaseOrder().getBuyer().getUserName().equalsIgnoreCase(userId) ||
    				state.getPurchaseOrder().getSeller().getUserName().equalsIgnoreCase(userId)){
    			returnRecords.add(state.getPurchaseOrder());
    		}
    	}
    	return returnRecords;
    }
    
    /**
     * Return the purchase order based on 'poNumber'.
     */
    @GET
    @Path("{poNumber}/get-po-details")
    @Produces(MediaType.APPLICATION_JSON)
    public List<PurchaseOrderNew> getPODetails(@PathParam("poNumber") String poNumber) {
    	
    	List<PurchaseOrderNew> returnRecords = new ArrayList<PurchaseOrderNew>();
    	
    	List<StateAndRef<ContractState>> allRecords = services.vaultAndUpdates().getFirst();
    	
    	for(int i=0; i<allRecords.size();i++){
    		
    		StateAndRef<ContractState> singleRecord = (StateAndRef<ContractState>) allRecords.get(i);
    		
    		PurchaseOrderState state = (PurchaseOrderState) singleRecord.getState().getData();
    		
    		if(state.getPurchaseOrder().getPurchaseOrderNum().equalsIgnoreCase(poNumber)){
    			returnRecords.add(state.getPurchaseOrder());
    		}
    	}
    	// return only one record based on date which is created last
    	PurchaseOrderNew lastPOCreation = Collections.max(returnRecords, Comparator.comparing(PurchaseOrderNew::getPoDate));
    	
    	returnRecords.clear();
    	returnRecords.add(lastPOCreation);
    	
        return returnRecords;
    }

    /**
     * This will create new purchase order     
     */
    @PUT
    @Path("{party}/create-purchase-order")
    @Produces(MediaType.APPLICATION_JSON)
    public Response createPurchaseOrder(PurchaseOrderNew purchaseOrder, @PathParam("party") String partyName) throws InterruptedException, ExecutionException {
    	System.out.println(purchaseOrder);
        
    	final Party otherParty = services.partyFromName("BankOfAmsterdam");
        System.out.println("Counter party......."+otherParty);       

        if (otherParty == null) {
            return Response.status(Response.Status.BAD_REQUEST).build();
        }       
        purchaseOrder.setPoDate(new Date());
        final PurchaseOrderState state = new PurchaseOrderState(
                purchaseOrder,
                services.nodeIdentity().getLegalIdentity(),
                otherParty, 
                //anotherParty,
                new PurchaseOrderContract());        
        // The line below blocks and waits for the flow to return.
        final DTCFlow.DTCFlowResult result = services
                //.startFlowDynamic(DTCFlow.Initiator.class, state, otherParty, anotherParty)
                .startFlowDynamic(DTCFlow.Initiator.class, state, otherParty)
                .getReturnValue()
                .toBlocking()
                .first();        
        final Response.Status status;
        if (result instanceof DTCFlow.DTCFlowResult.Success) {
            status = Response.Status.CREATED;
        } else {
            status = Response.Status.BAD_REQUEST;
        }

        return Response
                .status(status)
                .entity(result.toString())
                .build();
    }
    
    @PUT
    @Path("user-registration")
    @Produces(MediaType.APPLICATION_JSON)
    public User createUser(User user) throws InterruptedException, ExecutionException, SQLException {
    	System.out.println("*************************************************");
    	System.out.println(user);    
    	System.out.println("*************************************************");
    	int result = DatabaseUtil.createUser(myLegalName, user);
    	User returnUser = null;
        final Response.Status status;
        if (result != -1) {
            status = Response.Status.CREATED;
            returnUser = DatabaseUtil.retriveUser(myLegalName, user.getUserId());
        } else {
            status = Response.Status.BAD_REQUEST;
        }

        return returnUser;
    }
    
    @PUT
    @Path("validate-login")
    @Produces(MediaType.APPLICATION_JSON)
    public User validateLogin(User user) throws InterruptedException, ExecutionException, SQLException {
    	
    	System.out.println(user);        
    	boolean result = DatabaseUtil.validateLogin(myLegalName, user.getUserId(), user.getPassword());
    	
    	System.out.println("User validation status = "+ result);
    	
        final Response.Status status;
        User returnUser = null;
        if (result == true) {
            status = Response.Status.CREATED;
            returnUser = DatabaseUtil.retriveUser(myLegalName, user.getUserId());
        } else {
            status = Response.Status.BAD_REQUEST;
        }

        return returnUser;
    }
}