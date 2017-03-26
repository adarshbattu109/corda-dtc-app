package com.capgemini.dtc.app.flow;

import static kotlin.collections.CollectionsKt.single;

import java.security.KeyPair;
import java.time.Duration;
import java.time.Instant;
import java.util.Set;

import net.corda.core.crypto.CompositeKey;
import net.corda.core.crypto.CryptoUtilities;
import net.corda.core.crypto.DigitalSignature;
import net.corda.core.crypto.Party;
import net.corda.core.flows.FlowLogic;
import net.corda.core.transactions.SignedTransaction;
import net.corda.core.transactions.TransactionBuilder;
import net.corda.core.transactions.WireTransaction;
import net.corda.core.utilities.ProgressTracker;
import net.corda.core.utilities.UntrustworthyData;
import net.corda.flows.FinalityFlow;
import co.paralleluniverse.fibers.Suspendable;

import com.capgemini.dtc.app.state.PurchaseOrderState;
import com.google.common.collect.ImmutableSet;


public class DTCFlow {
    public static class Initiator extends FlowLogic<DTCFlowResult> {

        private final PurchaseOrderState purchaseOrderState;
        private final Party otherParty;
        
        private final ProgressTracker progressTracker = new ProgressTracker(
                CONSTRUCTING_OFFER,
                SENDING_OFFER_AND_RECEIVING_PARTIAL_TRANSACTION,
                VERIFYING,
                SIGNING,
                NOTARY,
                RECORDING,
                SENDING_FINAL_TRANSACTION
        );

        private static final ProgressTracker.Step CONSTRUCTING_OFFER = new ProgressTracker.Step(
                "Constructing proposed purchase order.");
        private static final ProgressTracker.Step SENDING_OFFER_AND_RECEIVING_PARTIAL_TRANSACTION = new ProgressTracker.Step(
                "Sending purchase order to seller for review, and receiving partially signed transaction from seller in return.");
        private static final ProgressTracker.Step VERIFYING = new ProgressTracker.Step(
                "Verifying signatures and contract constraints.");
        private static final ProgressTracker.Step SIGNING = new ProgressTracker.Step(
                "Signing transaction with our private key.");
        private static final ProgressTracker.Step NOTARY = new ProgressTracker.Step(
                "Obtaining notary signature.");
        private static final ProgressTracker.Step RECORDING = new ProgressTracker.Step(
                "Recording transaction in vault.");
        private static final ProgressTracker.Step SENDING_FINAL_TRANSACTION = new ProgressTracker.Step(
                "Sending fully signed transaction to seller.");
      
        public Initiator(PurchaseOrderState purchaseOrderState, Party otherParty) {
            this.purchaseOrderState = purchaseOrderState;
            this.otherParty = otherParty;            
        }

        @Override public ProgressTracker getProgressTracker() { return progressTracker; }

        /**
         * The flow logic is encapsulated within the call() method.
         */
        @Suspendable
        @Override public DTCFlowResult call() {
        	
        	System.out.println("Flow started by Initiator...........");
        	
            try {

                final KeyPair myKeyPair = getServiceHub().getLegalIdentityKey();
                // Obtain a reference to the notary we want to use and its public key.
                final Party notary = single(getServiceHub().getNetworkMapCache().getNotaryNodes()).getNotaryIdentity();
                final CompositeKey notaryPubKey = notary.getOwningKey();

                final TransactionBuilder utx = purchaseOrderState.generateAgreement(notary);

                final Instant currentTime = getServiceHub().getClock().instant();
                utx.setTime(currentTime, Duration.ofSeconds(30));               

                final SignedTransaction signWithA = utx.signWith(myKeyPair).toSignedTransaction(false);
                
                final SignedTransaction signWithB = this.sendAndReceive(SignedTransaction.class, otherParty , signWithA).unwrap(data -> data);
                
                final WireTransaction wtxWithB = signWithB.verifySignatures(notaryPubKey,this.otherParty.getOwningKey());
                
                wtxWithB.toLedgerTransaction(getServiceHub()).verify();

                final Set<Party> participants = ImmutableSet.of(getServiceHub().getMyInfo().getLegalIdentity(), otherParty);
                // FinalityFlow() notarises the transaction and records it in each party's vault.
                subFlow(new FinalityFlow(signWithB, participants),false);

                return new DTCFlowResult.Success(String.format("Transaction id %s committed to ledger.", signWithB.getId()));

            } catch(Exception ex) {
                // Just catch all exception types.
                return new DTCFlowResult.Failure(ex.getMessage());
            }
        
        }
    }

    public static class Acceptor extends FlowLogic<DTCFlowResult> {

        private final Party otherParty;
        private final ProgressTracker progressTracker = new ProgressTracker(
                WAIT_FOR_AND_RECEIVE_PROPOSAL,
                GENERATING_TRANSACTION,
                SIGNING,
                SEND_TRANSACTION_AND_WAIT_FOR_RESPONSE,
                VERIFYING_TRANSACTION,
                RECORDING
        );

        private static final ProgressTracker.Step WAIT_FOR_AND_RECEIVE_PROPOSAL = new ProgressTracker.Step(
                "Receiving proposed purchase order from buyer.");
        private static final ProgressTracker.Step GENERATING_TRANSACTION = new ProgressTracker.Step(
                "Generating transaction based on proposed purchase order.");
        private static final ProgressTracker.Step SIGNING = new ProgressTracker.Step(
                "Signing proposed transaction with our private key.");
        private static final ProgressTracker.Step SEND_TRANSACTION_AND_WAIT_FOR_RESPONSE = new ProgressTracker.Step(
                "Sending partially signed transaction to buyer and wait for a response.");
        private static final ProgressTracker.Step VERIFYING_TRANSACTION = new ProgressTracker.Step(
                "Verifying signatures and contract constraints.");
        private static final ProgressTracker.Step RECORDING = new ProgressTracker.Step(
                "Recording transaction in vault.");

        public Acceptor(Party otherParty) {
            this.otherParty = otherParty;
        }

        @Override public ProgressTracker getProgressTracker() { return progressTracker; }

        @Suspendable
        @Override public DTCFlowResult call() {
        	try {
        		System.out.println("Acceptor [" + getServiceHub().getMyInfo().getLegalIdentity() +"] received request.............");
                
                final KeyPair keyPair = getServiceHub().getLegalIdentityKey();

                // Obtain a reference to the notary we want to use and its public key.
                final Party notary = single(getServiceHub().getNetworkMapCache().getNotaryNodes()).getNotaryIdentity();
                final CompositeKey notaryPubKey = notary.getOwningKey();

                final SignedTransaction utx = this.receive(SignedTransaction.class, otherParty).unwrap(data -> data );

                final DigitalSignature.WithKey mySig = CryptoUtilities.signWithECDSA(keyPair, utx.getTx().getId().getBytes());
                final SignedTransaction vtx = utx.plus(mySig);

                this.send(otherParty, vtx);                

                return new DTCFlowResult.Success(String.format("Signed And Sent Back"));

            } catch (Exception ex) {
                return new DTCFlowResult.Failure(ex.getMessage());
            }
        }
    }

    public static class DTCFlowResult {
        public static class Success extends com.capgemini.dtc.app.flow.DTCFlow.DTCFlowResult {
            private String message;

            private Success(String message) { this.message = message; }

            @Override
            public String toString() { return String.format("Success(%s)", message); }
        }

        public static class Failure extends com.capgemini.dtc.app.flow.DTCFlow.DTCFlowResult {
            private String message;

            private Failure(String message) { this.message = message; }

            @Override
            public String toString() { return String.format("Failure(%s)", message); }
        }
    }
}