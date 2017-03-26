package com.capgemini.dtc.app.flow;

import java.security.KeyPair;
import java.time.Duration;
import java.time.Instant;
import java.util.Currency;
import java.util.HashMap;
import java.util.Map;

import net.corda.contracts.CommercialPaper;
import net.corda.core.contracts.Amount;
import net.corda.core.contracts.Issued;
import net.corda.core.contracts.StateAndRef;
import net.corda.core.crypto.CompositeKey;
import net.corda.core.crypto.Party;
import net.corda.core.crypto.SecureHash;
import net.corda.core.flows.FlowLogic;
import net.corda.core.node.NodeInfo;
import net.corda.core.transactions.SignedTransaction;
import net.corda.core.utilities.ProgressTracker;
import net.corda.flows.TwoPartyTradeFlow;
import co.paralleluniverse.fibers.Suspendable;


public class SellerFlow extends FlowLogic<SignedTransaction> {

	private final Party otherParty;
	private final Amount<Currency> price;

	private final ProgressTracker progressTracker = new ProgressTracker(
			SELF_ISSUING, TRADING);

	private static final ProgressTracker.Step SELF_ISSUING = new ProgressTracker.Step(
			"Got session ID back, issuing and timestamping some commercial paper");
	private static final ProgressTracker.Step TRADING = new ProgressTracker.Step(
			"Starting the trade flow") {
		public ProgressTracker childProgressTracker() {
			return TwoPartyTradeFlow.Seller.Companion.tracker();
		}

	};

	private static final net.corda.core.crypto.SecureHash.SHA256 PROSPECTUS_HASH;

	static {
		/** SHA-256 hash value of 'R-3083.zip' file src/main/resources */
		PROSPECTUS_HASH = SecureHash.Companion
				.parse("8d88af2dbc7c224c912906815b3859cd8ec848b410d226c912cb12a2d2c20588");
	}

	public SellerFlow(Party otherParty, Amount<Currency> amount) {
		this.otherParty = otherParty;
		this.price = amount;
	}

	@Override
	public ProgressTracker getProgressTracker() {
		return progressTracker;
	}

	/**
	 * The flow logic is encapsulated within the call() method.
	 */
	@Suspendable
	@Override
	public SignedTransaction call() {

		progressTracker.setCurrentStep(SELF_ISSUING);

		final NodeInfo notary = getServiceHub().getNetworkMapCache().getNotaryNodes().get(0);
		final KeyPair cpOwnerKey = getServiceHub().getLegalIdentityKey();

		StateAndRef<CommercialPaper.State> assetToSell = selfIssueSomeCommercialPaper(
				getServiceHub().getMyInfo().getLegalIdentity().getOwningKey(),
				notary);
		
		progressTracker.setCurrentStep(TRADING);
		
		// Send the offered price to buyer.
        send(otherParty, price);
        
        TwoPartyTradeFlow.Seller seller = new TwoPartyTradeFlow.Seller(
                otherParty,
                notary,
                assetToSell,
                price,
                cpOwnerKey,
                progressTracker.getChildProgressTracker(TRADING));

        return subFlow(seller, true);

	}

	@Suspendable
	public StateAndRef<CommercialPaper.State> selfIssueSomeCommercialPaper(
			CompositeKey ownedBy, NodeInfo notaryNode) {
		
		// Make a fake company that's issued its own paper.
		KeyPair keyPair = net.corda.core.crypto.CryptoUtilities.generateKeyPair();
		Party party = new Party("Bank of London", keyPair.getPublic());		
		
		Currency currency = Currency.getInstance("USD");
		//Issued issued = new Issued(DUMMY_CASH_ISSUER, currency);
		
		//Map<String,Map<String,String>> initStringStringMap = new Map<String,Map<String,String>>{'one' => new Map<String, String>{'a'=>'b', 'c'=> 'd'}, 'two' =>new Map<String, String> {'e'=>'f', 'g'=> 'h'} };
		
		Amount<Issued<Currency>> amount = null;		
		
		new CommercialPaper().generateIssue(party.ref(new Byte((byte)1)), amount, Instant.now().plus(Duration.ofDays(10)), notaryNode.getNotaryIdentity());
		
		

		return null;

	}

}
