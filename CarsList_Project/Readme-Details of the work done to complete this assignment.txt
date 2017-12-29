
Details of the work done to complete this assignment :

1. This assignment is built using Java and Spring framework with Maven dependency management.
	 The build will be generated in a war file and then is hosted in Tomcat.
	 It is tested in the local setup.

2. In order to Scrape from the source HTML of Craigslist website, 
	 I have used a HTML Scraping and Parsing Java Library known as "Jsoup".

3. The scraping is done in 2 steps. As Craigslist has a main page with all the listings,
	 for which every listing has a sub URL with details. So in Step 1, all the listings in the main URL is scraped.
	 Then in step 2, these sub URLs are scraped one by one and get the details of a particular car.
	 The scraped result is stored in the Java model object and then stored in the Database.

4. All of this scrapping and storing happens in the private constructor of the singleton service class, 
	 and thus will give delayed response for the 1st time the newly exposed API is hit. 
	 But once this is done, the API response later will come immediately.
	 So as the scraping and storing is done only once in one instance of the server running, 
	 if we restart the server, then it will do again.

7. The Database used - Apache Derby DB, is embedded in the same application. 
	 It runs in the same JVM as the application. Hence no need to start the DB Server separately for this application.

7. There is just one DB table with the following fields : 
		create table cardetails (
			carid integer not null GENERATED ALWAYS AS IDENTITY (START WITH 0, INCREMENT BY 1),
			brand varchar(50), 
			modelyear varchar(15), 
			color varchar(30),
			fuel varchar(15),
			condition varchar(30),
			transmission varchar(30),
			odometer varchar(20),
			titlestatus varchar(30),
			postingtitle varchar(80),
			CONSTRAINT primary_key PRIMARY KEY (carid)
		);

8. The API exposed is similar to as advised in the assignment question.
	 i.e. http://localhost:8080/CarsList/v1/search/brand?q=maruti
	 So the URL part which has "brand" is actually a Path Parameter of Category type and can be changed with these following allowed values only.
	 Further there is a Query Parameter also, named as "q" which takes the value to be searched.
	 Categories allowed are :
		 brand
		 model (indicates year of manufacture)
		 fuel
		 color
		 condition
		 transmission
		 odometer
		 titlestatus
		 title
	 Example URLs with all these categories are :
			http://localhost:8080/CarsList/v1/search/brand?q=hyundai
			http://localhost:8080/CarsList/v1/search/model?q=2015
			http://localhost:8080/CarsList/v1/search/fuel?q=diesel
			http://localhost:8080/CarsList/v1/search/color?q=blue
			http://localhost:8080/CarsList/v1/search/condition?q=good
			http://localhost:8080/CarsList/v1/search/transmission?q=automatic
			http://localhost:8080/CarsList/v1/search/odometer?q=10000
			http://localhost:8080/CarsList/v1/search/titlestatus?q=clean
			http://localhost:8080/CarsList/v1/search/title?q=SUV
			
9. When any of these queries are recieved through the API, I am searching DB with these 2 parameters of Category and Value.

10. I also attempted doing a faceted search using Lucene - the Java Library for search. 
	 I am able to index all the data after storing in the DB at the beginning, in the constructor. 
	 But when I do a search on the index, it is not returning any data. Hence I have commented that part of calling the searchIndex() method.
	 If it was working, there could be another Endpoint exposed to give the Faceted search results.
	 

	 
