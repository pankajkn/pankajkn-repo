package com.jio.carslist;

import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class CarController {
	
	@RequestMapping(value= "v1/search/{category}", method=RequestMethod.GET, produces="application/json")
	public CarDetailsResponse getCars(@PathVariable String category, @RequestParam("q") String query ) {
		CarListService service = CarListService.getInstance();
		return service.getCars(category, query);
	}
}
