package com.jio.carslist;

import java.util.List;

public class CarDetailsResponse {

	private String message;
	
	private List<CarDetailsModel> carDetails;

	public String getMessage() {
		return message;
	}

	public void setMessage(String message) {
		this.message = message;
	}

	public List<CarDetailsModel> getCarDetails() {
		return carDetails;
	}

	public void setCarDetails(List<CarDetailsModel> carDetails) {
		this.carDetails = carDetails;
	}
}
