/**
 * 
 */
package com.capgemini.dtc.app.model;

import java.util.Date;
import java.util.List;
import java.util.Random;

import com.fasterxml.jackson.annotation.JsonFormat;

/**
 * @author Balaji
 *
 */
public class TrackOrderNew {

	/**
	 * 
	 */
	public TrackOrderNew() {
		super();
		// TODO Auto-generated constructor stub
	}

	//private Party buyer;
	//private Party seller;
	private String purchaseOrderNum;
	private String trackingID;
	//private double totalPrice;
	//private String currency;
	//@JsonFormat(pattern = "yyyy-MM-dd")
	//private Date deliveryDate;
	//private String incoterm;
	//private int pymntCondDays;
	private List<ItemPurchased> itemsList;
	private Address deliveryAddress;
	//private String pymntConfirmation;
	//private String bankPymntCommitment;
	//private String infoCounterparty;
	//private String forfaitingOfInvoice;
	private List<String> partner = new ArrayList<String>();
			partner.add("DHL");
			partner.add("UPS");
			partner.add("ADS");
			partner.add("BlueDart");
	Date poDate;
	
	/**
	 * @param buyer
	 * @param seller
	 * @param purchaseOrderNum
	 * @param totalPrice
	 * @param currency
	 * @param deliveryDate
	 * @param incoterm
	 * @param pymntCondDays
	 * @param itemsList
	 * @param deliveryAddress
	 * @param isPymntConfirmation
	 * @param isBankPymntCommitment
	 * @param isInfoCounterparty
	 * @param isForfaitingOfInvoice
	 */
	public TrackOrderNew(String purchaseOrderNum, List<ItemPurchased> itemsList,
			Address deliveryAddress) {
		super();
		//this.buyer = buyer;
		//this.seller = seller;
		this.purchaseOrderNum = purchaseOrderNum;
		
		//Get Random Courier Partner
		Random randomizer = new Random();
		String random = partner.get(randomizer.nextInt(partner.size()));
		//Get Unique Tracking ID
		Random rand = new Random();
		int num = rand.nextInt(9000000) + 1000000;
		this.trackingID = random + '_' + num;
		
		
		//this.totalPrice = totalPrice;
		//this.currency = currency;
		//this.deliveryDate = deliveryDate;
		//this.incoterm = incoterm;
		//this.pymntCondDays = pymntCondDays;
		this.itemsList = itemsList;
		this.deliveryAddress = deliveryAddress;
		//this.pymntConfirmation = isPymntConfirmation;
		//this.bankPymntCommitment = isBankPymntCommitment;
		//this.infoCounterparty = isInfoCounterparty;
		//this.forfaitingOfInvoice = isForfaitingOfInvoice;
		this.poDate = new Date();
	}
	
	
	
	public Date getPoDate(){
		return poDate;
	}
	
	public void setPoDate(Date poDate){
		this.poDate = poDate;
	}
	
	/**
	 * @return the purchaseOrderNum
	 */
	public String getPurchaseOrderNum() {
		return purchaseOrderNum;
	}
	/**
	 * @param purchaseOrderNum the purchaseOrderNum to set
	 */
	public void setPurchaseOrderNum(String purchaseOrderNum) {
		this.purchaseOrderNum = purchaseOrderNum;
	}

	/**
	 * @return the itemsList
	 */
	public List<ItemPurchased> getItemsList() {
		return itemsList;
	}
	/**
	 * @param itemsList the itemsList to set
	 */
	public void setItemsList(List<ItemPurchased> itemsList) {
		this.itemsList = itemsList;
	}
	/**
	 * @return the deliveryAddress
	 */
	public Address getDeliveryAddress() {
		return deliveryAddress;
	}
	/**
	 * @param deliveryAddress the deliveryAddress to set
	 */
	public void setDeliveryAddress(Address deliveryAddress) {
		this.deliveryAddress = deliveryAddress;
	}
	
	/* (non-Javadoc)
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString() {
		return String.format(
				"TrackOrderNew [purchaseOrderNum=%s, trackingID=%s, itemsList=%s, deliveryAddress=%s]",
				purchaseOrderNum, itemsList,
				deliveryAddress);
	}
	
	
}
