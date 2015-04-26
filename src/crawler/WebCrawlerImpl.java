package crawler;

import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.sql.Connection;
import java.util.LinkedList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * This is an abstract implementation of the WebCrawler class.  It provides a
 * completed and final implementation of the required 'crawl()' method.  It
 * declares the 'search()' method to be abstract. A concrete extension of this
 * class is therefore required to fully implement the WebCrawler interface.
 * This is to allow for different implementations of the 'search()' method that
 * can still be consistently called by the 'crawl()' method.
 * 
 * @author James Hill
 */
public abstract class WebCrawlerImpl implements WebCrawler {
    
    /**
     * The maximum number of links that will be searched by the 'crawl' method.
     */
    private int maxLinks = 100;
    
    /**
     * The maximum depth of links that will be searched by the 'crawl' method.
     * The depth is the number of links away from the original start URL that
     * the crawler will search.
     */
    private int maxDepth = 2;
    
    /**
     * This integer records the number of links that have been processed by the
     * web crawler. It is used to compare against the maxLinks integer.
     */
    private int linksProcessed = 0;
    
    /**
     * This priority queue number is assigned to links on the temporary table.
     * The links will be given a priority number that reflects the depth of the
     * web page away from the initial URL provided to the class.
     */
    private int priority = 0;
    
    /**
     * This List will hold links scanned from a web page which can be iterated
     * through to print out to the database.
     */
    private List<String> links;
    
    /**
     * This is the basic constructor without parameters for the WebCrawler.
     * This is used when the programmer wishes to use the default values for
     * the maxLinks and maxDepth variables.
     */
    public WebCrawlerImpl(){}
    
    /**
     * This constructor allows changes to the values of the maxLinks variable
     * and/or the maxDepth variable.
     * 
     * @param maxLinks the maximum number of links to be processed.
     * @param maxDepth the maximum depth of web pages to be processed.
     */
    public WebCrawlerImpl(Integer maxLinks, Integer maxDepth){
        if(maxLinks > 0 || maxDepth > 0){
            this.maxLinks = maxLinks != null ? maxLinks : this.maxLinks;
            this.maxDepth = maxDepth != null ? maxDepth : this.maxDepth;
        }
    }
    
    @Override
    final public LinkedList<String> crawl(String startURL, Connection conn){
        // Setup all required variables and objects.
        LinkDB dataBase = new LinkDBImpl(conn);
        InputStream input;
        HyperlinkListBuilder builder;
        List<URL> linkList = new LinkedList<>();
        String URLstring = startURL;
        URL tempURL = null;
        
        // Try creating a URL object from the startURL.
        try {
            tempURL = new URL(startURL);
        } catch (MalformedURLException exc) {
            System.err.println("Error processing stream: " + exc);
        }
        
        // Loop through the links.
        do{
            if(!dataBase.checkExistsTemp(URLstring) &&
                    !URLstring.contentEquals("") && tempURL != null){
                try {
                    input = tempURL.openStream();
                    builder = new HyperlinkListBuilderImpl();
                    linkList = builder.createList(URLstring, input);
                    input.close();
                } catch (IOException exc) {
                    System.err.println("Error processing stream: " + exc);
                }
                
                // Enter non-duplicate URL's into the temp table.
                if(!linkList.isEmpty()){
                    for(URL link : linkList){
                        if(!dataBase.checkExistsTemp(link.getPath())){
                            dataBase.writeTemp(priority, URLstring);
                        }
                    }
                }
                
                // Enter results from 'search' onto the results table.
                if(search(tempURL)){
                    dataBase.writeResult(URLstring);
                }
            }
            
            // Prepare for next loop.
            dataBase.linkVisited(URLstring);
            URLstring = dataBase.getNextURL();
            linksProcessed++;
            priority++;
            
        } while(priority <= maxDepth && linksProcessed <= maxLinks);
        
        return dataBase.returnResults();
    }
    
    /**
     * This method has been designated abstract to allow different future
     * implementations. The most recent URL crawled can be passed to the method
     * with a list of strings the user would like to search the URL for. If any
     * strings are found a 'true' value should be returned; otherwise a
     * 'false' value should be returned.
     * 
     * @return the existence of the provided search terms in the URL.
     * @param currentURL the URL most recently process for links.
     * @param searchTerms terms being searched for in provided URL.
     */
    abstract public boolean search(URL currentURL, String...searchTerms);
}