package com.jio.carslist;

import java.io.File;
import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.Field.Store;
import org.apache.lucene.document.StringField;
import org.apache.lucene.facet.taxonomy.TaxonomyWriter;
import org.apache.lucene.facet.index.CategoryDocumentBuilder;
import org.apache.lucene.facet.search.FacetsCollector;
import org.apache.lucene.facet.search.params.CountFacetRequest;
import org.apache.lucene.facet.search.params.FacetSearchParams;
import org.apache.lucene.facet.search.results.FacetResult;
import org.apache.lucene.facet.search.results.FacetResultNode;
import org.apache.lucene.facet.taxonomy.CategoryPath;
import org.apache.lucene.facet.taxonomy.TaxonomyReader;
import org.apache.lucene.facet.taxonomy.directory.DirectoryTaxonomyReader;
import org.apache.lucene.facet.taxonomy.directory.DirectoryTaxonomyWriter;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.index.IndexWriterConfig.OpenMode;
import org.apache.lucene.index.Term;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.MultiCollector;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.TermQuery;
import org.apache.lucene.search.TopScoreDocCollector;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;
import org.apache.lucene.util.Version;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

public class CarListService {

	private static final String DERBY_DB_DIR = "derbyDBDir";
	private static final String DERBY_DB_NAME = "carsDB";
	private static String protocol = "jdbc:derby:";
	private static Connection dbConnection = null;
	
	
	private static final String INDEX_DIR = "facet/index";
	private static final String TAXONOMY_INDEX_DIR = "facet/taxoindex";
	private static Directory indexDir;
	private static Directory taxoDir;
	
	private static final CarListService INSTANCE = new CarListService();
	
	public static CarListService getInstance() {
		return INSTANCE;
	}

	private CarListService() {

		String craigsListMainURL  = "https://bangalore.craigslist.co.in/search/cta";
		Document jsoupDoc;
		Elements jsoupURLElements;
		List<String> craigsListSubURLs = new ArrayList<String>();
		List<CarDetailsModel> carDetailsRespList = new ArrayList<CarDetailsModel>();
		
		try {
			//Connects to Craigslist and gets the main page with search result
			jsoupDoc = Jsoup.connect(craigsListMainURL).get();
			
			//Scrapes the main HTML page got, using <li class="result-row" along with its immediate <a href> element to get URL. 
			jsoupURLElements = jsoupDoc.select("li.result-row a[href].result-image");

			//Loop through all the listed Car Items from the Main HTML and gets their URL to further get data
			for(Element jsoupElement : jsoupURLElements) {
				
				System.out.println("Selected element from main html is : "+jsoupElement.toString());
				
				//Gets the URL from the selected href element and adds it to the List.
				System.out.println("The extracted URL is :"+jsoupElement.absUrl("href"));
				craigsListSubURLs.add(jsoupElement.absUrl("href"));
			}

			//Loop through the URL list of Car listings and get more data of individual Car
			for(String carURL : craigsListSubURLs) {
				
				//Connect to individual Car listing in Craigslist
				jsoupDoc = Jsoup.connect(carURL).get();
				
				CarDetailsModel carDetails = new CarDetailsModel();
				
				Element postingTitle = jsoupDoc.selectFirst("h2.postingtitle span.postingtitletext span#titletextonly");
				if(postingTitle != null) {
					System.out.println("The posting title is :"+postingTitle.text());
					carDetails.setPostingTitle(postingTitle.text());
				}
				
				Elements spanElements = jsoupDoc.select("div.mapAndAttrs span");
				for(Element eachSpanEle : spanElements) {
					if(eachSpanEle.text().startsWith("condition:")) {
						carDetails.setCarCondition(eachSpanEle.select("b").text());
						System.out.println("The car condition is : "+carDetails.getCarCondition());
					}
					else if(eachSpanEle.text().startsWith("fuel:")) {
						carDetails.setCarFuel(eachSpanEle.select("b").text());
						System.out.println("The car fuel is : "+carDetails.getCarFuel());
					}
					else if(eachSpanEle.text().startsWith("odometer:")) {
						carDetails.setCarOdometer(eachSpanEle.select("b").text());
						System.out.println("The car odometer is : "+carDetails.getCarOdometer());
					}
					else if(eachSpanEle.text().startsWith("title status:")) {
						carDetails.setCarTitleStatus(eachSpanEle.select("b").text());
						System.out.println("The car condition is : "+carDetails.getCarCondition());
					}
					else if(eachSpanEle.text().startsWith("transmission:")) {
						carDetails.setCarTransmission(eachSpanEle.select("b").text());
						System.out.println("The car transmission is : "+carDetails.getCarTransmission());
					}
					else if(eachSpanEle.text().startsWith("paint color:")) {
						carDetails.setCarColor(eachSpanEle.select("b").text());
						System.out.println("The car paint color is : "+carDetails.getCarColor());
					}
					else {
						String modelBrand = eachSpanEle.select("b").text();
						System.out.println("The car model and year is : "+modelBrand);
						if(modelBrand.indexOf(" ") == -1) {
							System.out.println("The car's year is not given in proper format. So this car would not have the year of model.");
							carDetails.setCarBrand(modelBrand);
						}
						else {
							carDetails.setCarModelYear(modelBrand.trim().substring(0, modelBrand.indexOf(" ")));
							carDetails.setCarBrand(modelBrand.trim().substring(modelBrand.indexOf(" ")+1));
						}
					}
				}
				carDetailsRespList.add(carDetails);
			}
		}
		catch(IOException ioe) {
			ioe.printStackTrace();
		}
		writeDataToDerbyDB(carDetailsRespList);
		writeIndexDataWithLucene(carDetailsRespList);
	}
	
	private void writeIndexDataWithLucene(List<CarDetailsModel> carDetailsRespList) {

		try {
			indexDir = FSDirectory.open(new File(INDEX_DIR));
			taxoDir = FSDirectory.open(new File(TAXONOMY_INDEX_DIR));
			Analyzer analyzer = new StandardAnalyzer(Version.LUCENE_40);
			IndexWriterConfig iwc = new IndexWriterConfig(Version.LUCENE_40, analyzer);
			iwc.setOpenMode(OpenMode.CREATE);
			IndexWriter indexWriter = new IndexWriter(indexDir, iwc);
			TaxonomyWriter taxoWriter = new DirectoryTaxonomyWriter(taxoDir, OpenMode.CREATE_OR_APPEND);

			createFacetedCategoryDocuments(indexWriter, taxoWriter, carDetailsRespList);
			
			System.out.println("The indexing is complete. Ready for Search..");
			taxoWriter.commit();
			indexWriter.commit();
			indexWriter.close();
			taxoWriter.close();
		}
		catch(IOException ioe) {
			ioe.printStackTrace();
		}
	}
	
	
	private static void createFacetedCategoryDocuments(final IndexWriter indexWriter, final TaxonomyWriter taxoWriter, final List<CarDetailsModel> carList)
			throws IOException {
		
		for (final CarDetailsModel carDetails : carList) {
			org.apache.lucene.document.Document doc = new org.apache.lucene.document.Document();
			doc.add(new StringField("title", (carDetails.getPostingTitle() == null) ? "NoTitle" : carDetails.getPostingTitle(), Store.YES));
			doc.add(new StringField("brand", (carDetails.getCarBrand() == null) ? "NoBrand" : carDetails.getCarBrand(), Store.YES));
			List<CategoryPath> categories = new ArrayList<CategoryPath>();
			categories.add(new CategoryPath("model", (carDetails.getCarModelYear() == null) ? "NoModel" : carDetails.getCarModelYear()));
			categories.add(new CategoryPath("fuel", (carDetails.getCarFuel() == null) ? "NoFuel" : carDetails.getCarFuel()));
			categories.add(new CategoryPath("transmission", (carDetails.getCarTransmission() == null) ? "NoTransmission" : carDetails.getCarTransmission()));
			categories.add(new CategoryPath("color", (carDetails.getCarColor() == null) ? "NoColor" : carDetails.getCarColor()));
			categories.add(new CategoryPath("brand", (carDetails.getCarBrand() == null) ? "NoBrand" : carDetails.getCarBrand()));
			categories.add(new CategoryPath("condition", (carDetails.getCarCondition() == null) ? "NoCondition" : carDetails.getCarCondition()));
			categories.add(new CategoryPath("odometer", (carDetails.getCarOdometer() == null) ? "NoOdometer" : carDetails.getCarOdometer()));
			categories.add(new CategoryPath("titlestatus", (carDetails.getCarTitleStatus() == null) ? "NoTitleStatus" : carDetails.getCarTitleStatus()));
			CategoryDocumentBuilder categoryDocumentBuilder = new CategoryDocumentBuilder(taxoWriter);
			categoryDocumentBuilder.setCategoryPaths(categories);
			categoryDocumentBuilder.build(doc);
			indexWriter.addDocument(doc);
		}
		System.out.println("Adding all the documents or data to the index is over..");
	}
	
	private List<CarDetailsModel> searchIndex(String category, String value) {
		
		List<CarDetailsModel> carDetailsModelList = new ArrayList<CarDetailsModel>();

		try { 
			//indexDir = FSDirectory.open(new File(INDEX_FILE));
			//taxoDir = FSDirectory.open(new File(TAXONOMY_INDEX_FILE));

			IndexReader indexReader = DirectoryReader.open(indexDir);
			IndexSearcher searcher = new IndexSearcher(indexReader);
			TaxonomyReader taxoReader = new DirectoryTaxonomyReader(taxoDir);
			Query q = new TermQuery(new Term(category, value));
			TopScoreDocCollector tdc = TopScoreDocCollector.create(10, true);
			FacetSearchParams facetSearchParams = new FacetSearchParams();
			facetSearchParams.addFacetRequest(new CountFacetRequest(new CategoryPath("title"), 10));
			facetSearchParams.addFacetRequest(new CountFacetRequest(new CategoryPath("brand"), 10));
			facetSearchParams.addFacetRequest(new CountFacetRequest(new CategoryPath("model"), 10));
			facetSearchParams.addFacetRequest(new CountFacetRequest(new CategoryPath("fuel"), 10));
			facetSearchParams.addFacetRequest(new CountFacetRequest(new CategoryPath("transmission"), 10));
			facetSearchParams.addFacetRequest(new CountFacetRequest(new CategoryPath("color"), 10));
			facetSearchParams.addFacetRequest(new CountFacetRequest(new CategoryPath("condition"), 10));
			facetSearchParams.addFacetRequest(new CountFacetRequest(new CategoryPath("odometer"), 10));
			facetSearchParams.addFacetRequest(new CountFacetRequest(new CategoryPath("titlestatus"), 10));

			FacetsCollector facetsCollector = new FacetsCollector(facetSearchParams, indexReader, taxoReader);
			searcher.search(q, MultiCollector.wrap(tdc, facetsCollector));
			List<FacetResult> resultList = facetsCollector.getFacetResults();
			System.out.println("Search for Cars with the category: "+category+ " and value : "+value+ ", returned categories : "+resultList.size());

			for (final FacetResult facetResult : resultList) {
				CarDetailsModel carDetails = new CarDetailsModel();
				FacetResultNode presentFacetResultNode = facetResult.getFacetResultNode();
				System.out.println("\nMatching "+ presentFacetResultNode.getLabel());
				carDetails.setSearchCategory(presentFacetResultNode.getLabel().toString());
				
				System.out.println("The number of sub results are : "+presentFacetResultNode.getNumSubResults());
				System.out.println("Ordinal : "+presentFacetResultNode.getOrdinal()+
						", Residue : "+presentFacetResultNode.getResidue()+
						", value : "+presentFacetResultNode.getValue());
				if(facetResult.getFacetResultNode().getSubResults() == null) {
					System.out.println("There are no Sub Results found in the Result Node.");
				}
				else {
					System.out.println("The result of search has returned elements : "+facetResult.getFacetResultNode().getSubResults().iterator().hasNext());
				}
				
				for (FacetResultNode frNode : facetResult.getFacetResultNode().getSubResults()) {
					System.out.println("Full Label : "+frNode.getLabel()+", Last component Label : "+frNode.getLabel().lastComponent()+", value : "+ frNode.getValue());
					// to fill object
				}
				carDetailsModelList.add(carDetails);
			}
		}
		catch(IOException ioe) {
			ioe.printStackTrace();
		}
		return carDetailsModelList;
	}
	
	
	private void writeDataToDerbyDB(List<CarDetailsModel> carDetailsModelList) {
		
		//connect to Derby DB
		Statement createStmt = null;
		Statement deleteStmt = null;
	    PreparedStatement insertPrprdStmt = null;
	    
	    String createTableQuery = "create table cardetails "
	    		+ "(carid integer not null GENERATED ALWAYS AS IDENTITY (START WITH 0, INCREMENT BY 1), "
	    		+ "brand varchar(50), modelyear varchar(15), color varchar(30), fuel varchar(15), "
	    		+ "condition varchar(30), transmission varchar(30), odometer varchar(20), "
	    		+ "titlestatus varchar(30), postingtitle varchar(80), CONSTRAINT primary_key PRIMARY KEY (carid) )";
	    
	    String insertQuery = "insert into cardetails "
	    		+ "(brand, modelyear, color, fuel, condition, "
	    		+ "transmission, odometer, titlestatus, postingtitle) "
	    		+ "values (?, ?, ?, ?, ?, ?, ?, ?, ?)";
	    
	    String deleteQuery = "delete from cardetails";
	    
	    try {
	    	DriverManager.registerDriver(new org.apache.derby.jdbc.EmbeddedDriver());
	    	dbConnection = DriverManager.getConnection(protocol + DERBY_DB_DIR + "/" + DERBY_DB_NAME + ";create=true");
	    	System.out.println("Connected to Derby DB and created database : " + DERBY_DB_DIR+"/"+DERBY_DB_NAME);
	    	
	    	//create table cardetails
	    	createStmt = dbConnection.createStatement();
	    	createStmt.execute(createTableQuery);
	    	
	    	System.out.println("Created Table cardetails");			
	    }
	    catch (SQLException sqle) {
            sqle.printStackTrace();
            //This means that there is existing table and data. So the data will be deleted.
            try {
            	deleteStmt = dbConnection.createStatement();
            	deleteStmt.execute(deleteQuery);
            	System.out.println("The old table data is deleted.");
            }
            catch(SQLException se) {
            	se.printStackTrace();
            	System.out.println("Unable to delete the existing table values.");
            }
        }
	    finally {
	    	// Release all open resources to avoid unnecessary memory usage
	    	try {
	    		if (createStmt != null) {
	    			createStmt.close();
	    			createStmt = null;
	    		}
	    		if (deleteStmt != null) {
	    			deleteStmt.close();
	    			deleteStmt = null;
	    		}
	    	} catch (SQLException sqle) {
	    		System.out.println("Could not release the resources, createStmt / deleteStmt");
	    		sqle.printStackTrace();
	    	}
	    }
	    
	    	
	    try {
	    	//Insert the data into the table using prepared statement
	    	insertPrprdStmt = dbConnection.prepareStatement(insertQuery);

	    	//write the data to DB by looping car details
	    	for(CarDetailsModel carDetailsModel : carDetailsModelList) {
	    		insertPrprdStmt.setString(1, carDetailsModel.getCarBrand());
	    		insertPrprdStmt.setString(2, carDetailsModel.getCarModelYear());
	    		insertPrprdStmt.setString(3, carDetailsModel.getCarColor());
	    		insertPrprdStmt.setString(4, carDetailsModel.getCarFuel());
	    		insertPrprdStmt.setString(5, carDetailsModel.getCarCondition());
	    		insertPrprdStmt.setString(6, carDetailsModel.getCarTransmission());
	    		insertPrprdStmt.setString(7, carDetailsModel.getCarOdometer());
	    		insertPrprdStmt.setString(8, carDetailsModel.getCarTitleStatus());
	    		insertPrprdStmt.setString(9, carDetailsModel.getPostingTitle());

	    		insertPrprdStmt.executeUpdate();
	    		System.out.println("Inserted Car Record in DB : "+ carDetailsModel.getPostingTitle());
	    	}
	    }
	    catch(SQLException sqle) {
	    	sqle.printStackTrace();
	    }
	    finally {
	    	try {
	    		if(insertPrprdStmt != null) {
	    			insertPrprdStmt.close();
	    			insertPrprdStmt = null;
	    		}
	    		if (dbConnection != null) {
	    			dbConnection.close();
	    			dbConnection = null;
	    		}
	    	}
	    	catch (SQLException sqle) {
	    		System.out.println("Could not release the resources, insertPrprdStmt");
	    		sqle.printStackTrace();
	    	}
	    }
		
		try {
            // In embedded mode, we need to shutdown the DB after use. The shutdown=true attribute shuts down Derby.
            DriverManager.getConnection("jdbc:derby:;shutdown=true");
        }
        catch (SQLException se) {
            if (( (se.getErrorCode() == 50000)
                    && ("XJ015".equals(se.getSQLState()) ))) {
                // we got the expected exception
                System.out.println("Derby shut down normally");
            }
            else {
                // if the error code or SQLState is different, we have
                // an unexpected exception (shutdown failed)
                System.err.println("Derby did not shut down normally");
                se.printStackTrace();
            }
        }
	}
	
	
	private List<CarDetailsModel> searchDB(String category, String value) {
		
		String columnName = "brand";
		Statement selectStmt = null;
		ResultSet resultSet = null;
		List<CarDetailsModel> responseObj = new ArrayList<CarDetailsModel>();
		
		if(category.equals("title")) {
			columnName = "postingtitle";
		}
		else if(category.equals("model")) {
			columnName = "modelyear";
		}
		else if(category.equals("brand") || category.equals("color") || category.equals("fuel") || 
				category.equals("condition") || category.equals("transmission") || 
				category.equals("odometer") || category.equals("titlestatus")) {
			columnName = category;
		}
		
		String selectQuery = "select "
	    		+ "brand, modelyear, color, fuel, condition, "
	    		+ "transmission, odometer, titlestatus, postingtitle "
	    		+ "from cardetails "
	    		+ "where "+columnName+" like '%"+value+"%'";
		
		try {
			DriverManager.registerDriver(new org.apache.derby.jdbc.EmbeddedDriver());
	    	dbConnection = DriverManager.getConnection(protocol + DERBY_DB_DIR + "/" + DERBY_DB_NAME);
	    	System.out.println("Connected to Derby DB : " + DERBY_DB_DIR+"/"+DERBY_DB_NAME);
	    	
	    	selectStmt = dbConnection.createStatement();
	    	resultSet = selectStmt.executeQuery(selectQuery);
	    	
	    	while(resultSet.next()) {
	    		CarDetailsModel carDetails = new CarDetailsModel();
	    		carDetails.setSearchCategory(category);
	    		carDetails.setCarBrand(resultSet.getString("brand"));
	    		carDetails.setCarModelYear(resultSet.getString("modelyear"));
	    		carDetails.setCarColor(resultSet.getString("color"));
	    		carDetails.setCarFuel(resultSet.getString("fuel"));
	    		carDetails.setCarCondition(resultSet.getString("condition"));
	    		carDetails.setCarTransmission(resultSet.getString("transmission"));
	    		carDetails.setCarOdometer(resultSet.getString("odometer"));
	    		carDetails.setCarTitleStatus(resultSet.getString("titlestatus"));
	    		carDetails.setPostingTitle(resultSet.getString("postingtitle"));
	    		responseObj.add(carDetails);
	    	}
	    	try {
                // In embedded mode, we need to shutdown the DB after use. The shutdown=true attribute shuts down Derby.
                DriverManager.getConnection("jdbc:derby:;shutdown=true");
            }
            catch (SQLException se) {
                if (( (se.getErrorCode() == 50000)
                        && ("XJ015".equals(se.getSQLState()) ))) {
                    // we got the expected exception
                    System.out.println("Derby shut down normally");
                }
                else {
                    // if the error code or SQLState is different, we have
                    // an unexpected exception (shutdown failed)
                    System.err.println("Derby did not shut down normally");
                    se.printStackTrace();
                }
            }
		}
		catch (SQLException sqle) {
            sqle.printStackTrace();
        }
	    finally {
	    	// Release all open resources to avoid unnecessary memory usage
	    	try {
	    		if (selectStmt != null) {
	    			selectStmt.close();
	    			selectStmt = null;
	    		}
	    		if(resultSet != null) {
	    			resultSet.close();
	    			resultSet = null;
	    		}
	    		if (dbConnection != null) {
	    			dbConnection.close();
	    			dbConnection = null;
	    		}
	    	} catch (SQLException sqle) {
	    		sqle.printStackTrace();
	    	}
	    }
		return responseObj;
	}
	
	
	
	/**
	 * 
	 * @param category
	 * @param query
	 * @return
	 */
	public CarDetailsResponse getCars(String category, String query) {
		
		CarDetailsResponse response = new CarDetailsResponse();
		if(query == null || query.trim().equals("")) {
			response.setMessage("Error : The query given is Invalid");
		}
		else {
					
			List<CarDetailsModel> carDetailsModelList = searchDB(category, query);
			
			//The Faceted search is not returning any value which was indexed earlier, and hence commented out.			
			//List<CarDetailsModel> carDetailsModelList = searchIndex(category, query);
			
			response.setCarDetails(carDetailsModelList);
			response.setMessage("Success");
		}
		return response;
	}
}
