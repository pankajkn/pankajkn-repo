package com.jio.carslist;

public class CarDetailsModel {

	private String searchCategory;
	
	private String postingTitle;
	
	private String carModelYear;
	
	private String carBrand;
	
	private String carCondition;
	
	private String carFuel;
	
	private String carOdometer;
	
	private String carTitleStatus;
	
	private String carTransmission;
	
	private String carColor;

	public String getSearchCategory() {
		return searchCategory;
	}

	public void setSearchCategory(String searchCategory) {
		this.searchCategory = searchCategory;
	}

	public String getPostingTitle() {
		return postingTitle;
	}

	public void setPostingTitle(String postingTitle) {
		this.postingTitle = postingTitle;
	}

	public String getCarModelYear() {
		return carModelYear;
	}

	public void setCarModelYear(String carModelYear) {
		this.carModelYear = carModelYear;
	}

	public String getCarBrand() {
		return carBrand;
	}

	public void setCarBrand(String carBrand) {
		this.carBrand = carBrand;
	}

	public String getCarCondition() {
		return carCondition;
	}

	public void setCarCondition(String carCondition) {
		this.carCondition = carCondition;
	}

	public String getCarFuel() {
		return carFuel;
	}

	public void setCarFuel(String carFuel) {
		this.carFuel = carFuel;
	}

	public String getCarOdometer() {
		return carOdometer;
	}

	public void setCarOdometer(String carOdometer) {
		this.carOdometer = carOdometer;
	}

	public String getCarTitleStatus() {
		return carTitleStatus;
	}

	public void setCarTitleStatus(String carTitleStatus) {
		this.carTitleStatus = carTitleStatus;
	}

	public String getCarTransmission() {
		return carTransmission;
	}

	public void setCarTransmission(String carTransmission) {
		this.carTransmission = carTransmission;
	}

	public String getCarColor() {
		return carColor;
	}

	public void setCarColor(String carColor) {
		this.carColor = carColor;
	}

	@Override
	public String toString() {
		return "CarDetailsModel [postingTitle=" + postingTitle + ", carModelYear=" + carModelYear + ", carBrand="
				+ carBrand + ", carCondition=" + carCondition + ", carFuel=" + carFuel + ", carOdometer=" + carOdometer
				+ ", carTitleStatus=" + carTitleStatus + ", carTransmission=" + carTransmission + ", carColor="
				+ carColor + "]";
	}

}
