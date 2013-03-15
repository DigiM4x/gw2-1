package org.salgar.igw2trade.domain;

public class AnalyzedObject {
	private int dataId = 0;
	private int buyOffer = 0;
	private int salesOffer = 0;
	private int demand = 0;
	private int vendorValue;
	private double profitability;
	private String name;

	public int getDataId() {
		return dataId;
	}

	public void setDataId(int dataId) {
		this.dataId = dataId;
	}

	public int getBuyOffer() {
		return buyOffer;
	}

	public void setBuyOffer(int buyOffer) {
		this.buyOffer = buyOffer;
	}

	public int getSalesOffer() {
		return salesOffer;
	}

	public void setSalesOffer(int salesOffer) {
		this.salesOffer = salesOffer;
	}

	public int getDemand() {
		return demand;
	}

	public void setDemand(int demand) {
		this.demand = demand;
	}

	public int getVendorValue() {
		return vendorValue;
	}

	public void setVendorValue(int vendorValue) {
		this.vendorValue = vendorValue;
	}

	public double getProfitability() {
		return profitability;
	}

	public void setProfitability(double profitability) {
		this.profitability = profitability;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}
}
