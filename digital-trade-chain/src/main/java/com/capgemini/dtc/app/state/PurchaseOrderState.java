package com.capgemini.dtc.app.state;

import static java.util.stream.Collectors.toList;

import java.lang.reflect.Constructor;
import java.security.PublicKey;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import net.corda.core.contracts.Command;
import net.corda.core.contracts.DealState;
import net.corda.core.contracts.TransactionType;
import net.corda.core.contracts.UniqueIdentifier;
import net.corda.core.crypto.CompositeKey;
import net.corda.core.crypto.Party;
import net.corda.core.transactions.TransactionBuilder;

import com.capgemini.dtc.app.contract.PurchaseOrderContract;
import com.capgemini.dtc.app.model.PurchaseOrder;
import com.capgemini.dtc.app.model.PurchaseOrderNew;
import com.capgemini.dtc.app.contract.PurchaseOrderContract.Commands.Place;

// TODO: Implement QueryableState and add ORM code (to match Kotlin example).

/**
 * The state object that we will use to record the agreement of a valid purchase order issued by a buyer to a seller.
 *
 * There are a few key state interfaces, the most fundamental of which is [ContractState]. We have defined other
 * interfaces for different requirements. In this case, we are implementing a [DealState] which defines a few helper
 * properties and methods for managing states pertaining to deals.
 */
public class PurchaseOrderState implements DealState {
    private final PurchaseOrderNew purchaseOrder;
    private final Party buyer;
    private final Party seller;
    //private final Party anotherParty;
    private final PurchaseOrderContract contract;
    private final UniqueIdentifier linearId;

    public PurchaseOrderState(PurchaseOrderNew purchaseOrder,
                              Party buyer,
                              Party seller,
                              //Party anotherParty,
                              PurchaseOrderContract contract)
    {
        this.purchaseOrder = purchaseOrder;
        this.buyer = buyer;
        this.seller = seller;
        //this.anotherParty = anotherParty;
        this.contract = contract;
        this.linearId = new UniqueIdentifier(
                purchaseOrder.getPurchaseOrderNum(),
                UUID.randomUUID());
    }

    public PurchaseOrderNew getPurchaseOrder() { return purchaseOrder; }
    public Party getBuyer() { return buyer; }
    public Party getSeller() { return seller; }
    @Override public PurchaseOrderContract getContract() { return contract; }
    @Override public UniqueIdentifier getLinearId() { return linearId; }
    @Override public String getRef() { return linearId.getExternalId(); }
    //@Override public List<Party> getParties() { return Arrays.asList(buyer, seller, anotherParty); }
    @Override public List<Party> getParties() { return Arrays.asList(buyer, seller); }
    @Override public List<CompositeKey> getParticipants() {
        return getParties()
                .stream()
                .map(Party::getOwningKey)
                .collect(toList());
    }

    /**
     * This returns true if the state should be tracked by the vault of a particular node. In this case the logic is
     * simple; track this state if we are one of the involved parties.
     */
    @Override public boolean isRelevant(Set<? extends PublicKey> ourKeys) {
        final List<PublicKey> partyKeys = getParties()
                .stream()
                .flatMap(party -> party.getOwningKey().getKeys().stream())
                .collect(toList());
        return ourKeys
                .stream()
                .anyMatch(partyKeys::contains);

    }

    /**
     * Helper function to generate a new Issue() purchase order transaction. For more details on building transactions
     * see the API for [TransactionBuilder] in the JavaDocs.
     * https://docs.corda.net/api/net.corda.core.transactions/-transaction-builder/index.html
     */
    @Override public TransactionBuilder generateAgreement(Party notary) {
    	//**ERROR Code**
        /*return new TransactionType.General.Builder(notary)
                .withItems(this, new Command(new Place(), getParticipants()));*/
    	Class memberClasses[] = TransactionType.General.class.getDeclaredClasses();    	
    	
    	Class classDefinition = memberClasses[0];
    	
    	TransactionBuilder builder = null;
    	try{
    		Constructor cons = classDefinition.getConstructor(Party.class);    		
    		Object obj = cons.newInstance(notary);
    		
    		TransactionBuilder tempBuilder = (TransactionBuilder) obj;
    		builder = tempBuilder.withItems(this, new Command(new Place(), getParticipants()));
    		
    		
    	}catch(Exception e){
    		e.printStackTrace();
    	} 
    	
    	return builder;    
    }
}