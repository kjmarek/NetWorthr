package com.serverApp.serverApp.controllers;

import com.serverApp.serverApp.models.*;
import com.serverApp.serverApp.other.ArticleRetrieval;
import com.serverApp.serverApp.repositories.AccountsRepository;
import com.serverApp.serverApp.repositories.ArticleRepository;
import com.serverApp.serverApp.repositories.StockRepository;
import com.serverApp.serverApp.repositories.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.web.bind.annotation.*;

import java.io.UnsupportedEncodingException;
import java.lang.reflect.Array;
import java.net.URL;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Rest Controller for the articles table
 * <br>
 * ENDPOINT /article/updateDB for updating the articles in the DB (see fetchNewArticles())<br>
 * ENDPOINT /article/getAll for getting every article in the DB (see getAll())<br>
 * ENDPOINT /article/adminGetAll for getting every article (specialized for admin) (see adminGetAll())<br>
 * ENDPOINT /article/getPersonal/{id} for getting the articles of a specific user (see getPersonal())<br>
 * ENDPOINT /article/archive/{id} for archiving an article (see archiveArticle()<br>
 * ENDPOINT /article/upvote/{userId}/{articleId} for upvoting an article (see upvoteArticle())<br>
 * ENDPOINT /article/downvote/{userId}/{articleId} for downvoting an article (see downvoteArticle())<br>
 *
 * @author Griffin Stout
 *
 */
@RestController
public class ArticleController {

    private ArrayList<String> keywords = new ArrayList<> (Arrays.asList("investment", "business", "trading", "real estate", "interest rates", "student loans", "saving", "savings account", "CD", "CD rates"));
    private String[] loanKeywords = {"interest rates", "student loans"};
    private String[] savingsAccountKeywords = {"saving", "savings account"};
    private String[] certificateOfDepositKeywords = {"CD", "CD rates"};
    private String[] realEstateKeywords = {"real estate"};
    private String[] StockKeyWords;
    private String[] noUserKeyWords = {"investment", "business", "trading"};

    /**
     * @Autowired repository to ArticleRepository
     */
    @Autowired
    ArticleRepository articleRepo;

    @Autowired
    StockRepository stockRepo;

    @Autowired
    UserRepository userRepo;

    /**
     * @Autowired repository to AccountsRepository
     */
    @Autowired
    AccountsRepository accountRepo;

    /**
     * updates the articles in the database every 5 hours
     */
    @RequestMapping("/article/updateDB")
    @Scheduled(fixedRate = 18000000)//5 hours
    public void fetchNewArticles(){
        //API key is b46a1992ed6c457bb31e58178813a3cd

        ArticleRetrieval r = new ArticleRetrieval();
        ArrayList<Article> articles;

        int insertCount = 0;
        StockKeyWords = stockRepo.getTickers();
        for(int i = 0; i < StockKeyWords.length; i++) {
            keywords.add(StockKeyWords[i]);
        }
        for(int i = 0; i < keywords.size(); i ++)
            try {
                URL articlesURL = new URL("https://newsapi.org/v2/everything?" +
                        "q=" + URLEncoder.encode(keywords.get(i), "UTF-8") + "&" +
                        "apiKey=b46a1992ed6c457bb31e58178813a3cd");
                //System.out.println(articlesURL.toString());
                articles = r.getFromURL(articlesURL);

                for (int j = 0; j < articles.size() && j < 5; j++) {
                    articles.get(j).setKeyword(keywords.get(i)); //set keyword
                    articles.get(j).setIsActive(1); //set to active
                    articles.get(j).setUserId(0);//autogenerated user
                    articles.get(j).setVotes(0);
                    if (articles.get(j).getDescription().length() > 150) {
                        //truncate description if too long
                        articles.get(j).setDescription(articles.get(j).getDescription().substring(0, 147) + "...");
                    }
                    /*articles.get(j).setDescription(articles.get(j).getDescription().replaceAll("\"", "\'"));
                    articles.get(j).setTitle(articles.get(j).getTitle().replaceAll("\"", "'"));

                    articles.get(j).setDescription(articles.get(j).getDescription().replaceAll("\'", "\'"));
                    articles.get(j).setTitle(articles.get(j).getTitle().replaceAll("\'", "\'"));

                    articles.get(j).setDescription(articles.get(j).getDescription().replaceAll("’", "\t’"));
                    articles.get(j).setTitle(articles.get(j).getTitle().replaceAll("’", "\t’"));*/

                    //System.out.println(articles.get(j).getTitle());

                    //articles.get(j).setDescription(URLEncoder.encode(articles.get(j).getDescription(), "UTF-8"));
                    //articles.get(j).setTitle(URLEncoder.encode(articles.get(j).getTitle(), "UTF-8"));

                    //System.out.println(articles.get(j).getTitle());
                    if (articleRepo.getDuplicates(articles.get(j).getUrl()) == 0) {
                        articleRepo.save(articles.get(j));
                        insertCount++;
                    }
                }
                articles = null;
            } catch (Exception e) {
                System.out.println("Error in listTitles" + e.getMessage());
            }
            //articleRepo.fixArticles();
        System.out.println("Fetching articles...\n" + insertCount + " articles inserted.");
    }

    /**
     * gets all of the articles in the database in json format
     * @return every article in the database
     * @throws UnsupportedEncodingException UnsupportedEncodingException
     */
    @GetMapping("/article/getAll")
    public String getAll() throws UnsupportedEncodingException {

        String rString = "{";

        Article[] articles = articleRepo.getAllArticles();
        rString += "\"numArticles\":\"" + articles.length +"\",";

        for(int i = 0; i < articles.length; i ++){
            if(i == articles.length - 1){
                rString =
                        rString
                                + "\"article"
                                + i
                                + "\": {"
                                + "\"id\":\""
                                + articles[i].getId()
                                + "\","
                                + "\"title\":\""
                                + URLEncoder.encode(articles[i].getTitle(), "UTF-8")
                                + "\","
                                + "\"votes\":\""
                                + articles[i].getVotes()
                                + "\","
                                + "\"description\":\""
                                + URLEncoder.encode(articles[i].getDescription(), "UTF-8")
                                + "\","
                                + "\"pictureUrl\":\""
                                + articles[i].getUrlToImage()
                                + "\","
                                + "\"url\":\""
                                + articles[i].getUrl()
                                + "\","
                                + "\"isActive\":\""
                                + articles[i].getIsActive()
                                + "\"}";
            } else {
                rString =
                        rString + "\"article"
                                + i
                                + "\": {"
                                + "\"id\":\""
                                + articles[i].getId()
                                + "\","
                                + "\"title\":\""
                                + URLEncoder.encode(articles[i].getTitle(), "UTF-8")
                                + "\","
                                + "\"votes\":\""
                                + articles[i].getVotes()
                                + "\","
                                + "\"description\":\""
                                + URLEncoder.encode(articles[i].getDescription(), "UTF-8")
                                + "\","
                                + "\"pictureUrl\":\""
                                + articles[i].getUrlToImage()
                                + "\","
                                + "\"url\":\""
                                + articles[i].getUrl()
                                + "\","
                                + "\"isActive\":\""
                                + articles[i].getIsActive()
                                + "\"},";
            }
        }
        rString += "}";
        return rString;
    }

    /**
     * returns all of the articles in a special format for the admin
     * @return special formatting for adrmin use
     * @throws UnsupportedEncodingException UnsupportedEncodingException
     */
    @GetMapping("/article/adminGetAll")
    public String adminGetAll() throws UnsupportedEncodingException {
        String rString = "{";

        Article[] articles = articleRepo.getAllArticles();
        rString += "\"numArticles\":\"" + articles.length +"\", \"articles\": [";

        for(int i = 0; i < articles.length; i ++){
            if(i == articles.length - 1){
                rString =
                        rString
                                + "{"
                                + "\"id\":\""
                                + articles[i].getId()
                                + "\","
                                + "\"title\":\""
                                + URLEncoder.encode(articles[i].getTitle(), "UTF-8")
                                + "\","
                                + "\"votes\":\""
                                + articles[i].getVotes()
                                + "\","
                                + "\"userId\":\""
                                + articles[i].getUserId()
                                + "\","
                                + "\"description\":\""
                                + URLEncoder.encode(articles[i].getDescription(), "UTF-8")
                                + "\","
                                + "\"pictureUrl\":\""
                                + articles[i].getUrlToImage()
                                + "\","
                                + "\"url\":\""
                                + articles[i].getUrl()
                                + "\","
                                + "\"isActive\":\""
                                + articles[i].getIsActive()
                                + "\"}]";
            } else {
                rString =
                        rString + "{"
                                + "\"id\":\""
                                + articles[i].getId()
                                + "\","
                                + "\"title\":\""
                                + URLEncoder.encode(articles[i].getTitle(), "UTF-8")
                                + "\","
                                + "\"votes\":\""
                                + articles[i].getVotes()
                                + "\","
                                + "\"userId\":\""
                                + articles[i].getUserId()
                                + "\","
                                + "\"description\":\""
                                + URLEncoder.encode(articles[i].getDescription(), "UTF-8")
                                + "\","
                                + "\"pictureUrl\":\""
                                + articles[i].getUrlToImage()
                                + "\","
                                + "\"url\":\""
                                + articles[i].getUrl()
                                + "\","
                                + "\"isActive\":\""
                                + articles[i].getIsActive()
                                + "\"},";
            }
        }
        rString += "}";
        return rString;
    }


    /**
     * returns a list of articles based on the accounts of a specific user
     * @param id id of the user
     * @return list of articles for that user
     * @throws UnsupportedEncodingException UnsupportedEncodingException
     */
    @GetMapping("/article/getPersonal/{id}")
    public String getPersonal(@PathVariable long id) throws UnsupportedEncodingException{
        Accounts[] accounts = accountRepo.getAccountsById(id);
        //System.out.println("Number of accounts: " + accounts.length);
        ArrayList<String> keywords = new ArrayList<>();
        if(id == -1) {
            keywords.addAll(Arrays.asList(noUserKeyWords));
        }
        String[] tickerList = stockRepo.getTickers();
        ArrayList<String> tickers = new ArrayList<>((Arrays.asList(tickerList)));
        boolean loansRetrieved = false, savingsAccountsRetrieved = false, CDsRetrieved = false, realEstateRetrieved = false;
        for(int i = 0; i < accounts.length; i ++){
            switch(accounts[i].getType()){
                case "Loan":
                    if (!loansRetrieved) {
                        keywords.addAll(Arrays.asList(loanKeywords));
                        loansRetrieved = true;
                    }
                    break;
                case "SavingsAccount":
                    if (!savingsAccountsRetrieved) {
                        keywords.addAll(Arrays.asList(savingsAccountKeywords));
                        savingsAccountsRetrieved = true;
                    }
                        break;
                case "CertificateOfDeposit":
                    if (!CDsRetrieved) {
                        keywords.addAll(Arrays.asList(certificateOfDepositKeywords));
                        CDsRetrieved = true;
                    }
                    break;
                case "RealEstate":
                    if(!realEstateRetrieved) {
                        keywords.addAll(Arrays.asList(realEstateKeywords));
                        realEstateRetrieved = true;
                    }
                    break;
                case "Stock":
                    Stock stock = stockRepo.getStock(accounts[i].getAccountId());
                    for(int j = 0; j < tickers.size(); j++) {
                        if(tickers.get(j).equals(stock.getTicker())) {
                            keywords.add(tickers.get(j));
                            tickers.remove(j);
                        }
                    }
                default:
                    //do nothing, account has no type or no keywords
                    break;
            }
        }
        ArrayList<Article> articles = new ArrayList<>();
        for (int i = 0; i < keywords.size(); i++) {
            articles.addAll(Arrays.asList(articleRepo.getAllArticlesWithKeyword(keywords.get(i))));
        }
        articles.addAll(Arrays.asList(articleRepo.getAllPopularArticles()));

        articles = removeDuplicates(articles);

        String rString = "{";
        if (articles.size() > 0) {
            rString += "\"numArticles\":\"" + articles.size() + "\",";
        }else{
            rString += "\"numArticles\":\"" + articles.size() + "\"";
        }

        for(int i = 0; i < articles.size(); i ++){
            if(i == articles.size() - 1){
                rString =
                        rString
                                + "\"article"
                                + i
                                + "\": {"
                                + "\"id\":\""
                                + articles.get(i).getId()
                                + "\","
                                + "\"title\":\""
                                + URLEncoder.encode(articles.get(i).getTitle(), "UTF-8")
                                + "\","
                                + "\"votes\":\""
                                + articles.get(i).getVotes()
                                + "\","
                                + "\"vote\":\""
                                + getUserVote(id, articles.get(i))
                                + "\","
                                + "\"description\":\""
                                + URLEncoder.encode(articles.get(i).getDescription(), "UTF-8")
                                + "\","
                                + "\"pictureUrl\":\""
                                + articles.get(i).getUrlToImage()
                                + "\","
                                + "\"url\":\""
                                + articles.get(i).getUrl()
                                + "\","
                                + "\"isActive\":\""
                                + articles.get(i).getIsActive()
                                + "\"}";
            } else {
                rString =
                        rString + "\"article"
                                + i
                                + "\": {"
                                + "\"id\":\""
                                + articles.get(i).getId()
                                + "\","
                                + "\"title\":\""
                                + URLEncoder.encode(articles.get(i).getTitle(), "UTF-8")
                                + "\","
                                + "\"votes\":\""
                                + articles.get(i).getVotes()
                                + "\","
                                + "\"vote\":\""
                                + getUserVote(id, articles.get(i))
                                + "\","
                                + "\"description\":\""
                                + URLEncoder.encode(articles.get(i).getDescription(), "UTF-8")
                                + "\","
                                + "\"pictureUrl\":\""
                                + articles.get(i).getUrlToImage()
                                + "\","
                                + "\"url\":\""
                                + articles.get(i).getUrl()
                                + "\","
                                + "\"isActive\":\""
                                + articles.get(i).getIsActive()
                                + "\"},";
            }
        }
        rString += "}";
        return rString;
    }

    /**
     * archives an article
     * @param id if of article to archive
     * @return success message
     */
    @DeleteMapping("/article/archive/{id}")
    public String archiveArticle(@PathVariable int id){
        articleRepo.deleteArticle(id);
        return "{\"message\":\"success\"}";
    }

    /**
     * upvotes an article
     * @param userId user who is upvoting
     * @param articleId article to upvote
     * @return the vote of the user (1,0 ir -1)
     */
    @RequestMapping("/article/upvote/{userId}/{articleId}")
    public String upvoteArticle(@PathVariable long userId, @PathVariable long articleId){
        if(userRepo.checkUserInDb(userId) == 1){
            return "{\"error\":\"user + \'" + userId +"\' does not exist\"}";
        }
        if(articleRepo.getNumArticles(articleId) == 1){
            Article article = articleRepo.getOne(articleId);
            int vote = getUserVote(userId, article);
            int newVote = vote;
            int pos = -1;
            if(vote > -2){
                pos = getUserPosition(userId, article);
            }
            switch(vote){
                case 1://toggle back to 0
                    article.getVoters().get(pos).setVote(0);
                    article.setVotes(article.getVotes() - 1);
                    newVote = 0;
                    break;
                case 0:
                    article.getVoters().get(pos).setVote(1);
                    article.setVotes(article.getVotes() + 1);
                    newVote = 1;
                    break;
                case -1:
                    article.getVoters().get(pos).setVote(1);
                    article.setVotes(article.getVotes() + 2); // + 2 because they changed from a negative vote to positive
                    newVote = 1;
                    break;
                default://add new voter to article list
                    article.getVoters().add(new Vote(userId, 1, article));
                    article.setVotes(article.getVotes() + 1);
                    newVote = 1;
                    break;

            }

            /*articleRepo.updateArticleVoters(article.getVoters(), articleId);
            articleRepo.updateArticleVotes(article.getVotes(), articleId);*/

            articleRepo.save(article);
            return "{\"error\":\"none\"," +
                    "\"userId\":\"" + userId +"\"," +
                    "\"currentVote\":\"" + newVote + "\"," +
                    "\"articleVotes\":\"" + article.getVotes() +"\"}";

        }

        return "{\"error\":\"article not found\"}";
    }

    /**
     * downvotes an article
     * @param userId user who is downvoting
     * @param articleId article to downvote
     * @return the vote of the user (1,0 ir -1)
     */
    @RequestMapping("/article/downvote/{userId}/{articleId}")
    public String downvoteArticle(@PathVariable long userId, @PathVariable long articleId){
        if(articleRepo.getNumArticles(articleId) == 1){
            Article article = articleRepo.getOne(articleId);

            int vote = getUserVote(userId, article);
            int newVote = vote;
            int pos = -1;
            if(vote > -2){
                pos = getUserPosition(userId, article);
            }
            switch(vote){
                case 1:
                    article.getVoters().get(pos).setVote(-1);
                    article.setVotes(article.getVotes() - 2);
                    newVote = -1;
                    break;
                case 0:
                    article.getVoters().get(pos).setVote(-1);
                    article.setVotes(article.getVotes() - 1);
                    newVote = -1;
                    break;
                case -1://toggle back to 0
                    article.getVoters().get(pos).setVote(0);
                    article.setVotes(article.getVotes() + 1); // + 2 because they changed from a negative vote to positive
                    newVote = 0;
                    break;
                default://add new voter to article list
                    article.getVoters().add(new Vote(userId, -1, article));
                    article.setVotes(article.getVotes() - 1);
                    newVote = -1;
                    break;

            }

            /*articleRepo.updateArticleVoters(article.getVoters(), articleId);

            articleRepo.updateArticleVotes(article.getVotes(), articleId);*/

            articleRepo.save(article);
            return "{\"error\":\"none\"," +
                    "\"userId\":\"" + userId +"\"," +
                    "\"currentVote\":\"" + newVote + "\"," +
                    "\"articleVotes\":\"" + article.getVotes() +"\"}";

        }

        return "{\"error\":\"article not found\"}";
    }

    /**
     * This method searches the voters list of a certain article for a certain user and returns
     * their vote or -2 if they were not found. (-2 because -1 is an option for a vote)
     * @param userId userId
     * @param article article
     * @return vote or -2 if not found
     */
    public int getUserVote(long userId, Article article){
        List<Vote> voters = article.getVoters();

        //linear search RIP server
        for(int i = 0; i < voters.size(); i ++){
            if(voters.get(i).getUserId() == userId){
                return voters.get(i).getVote();
            }
        }
        return -2;
    }

    /**
     * This method searches the voters list of a certain article for a certain user and returns
     * their index (-1 if not found)
     * @param userId userId
     * @param article article
     * @return index or -1 if not found
     */
    public int getUserPosition(long userId, Article article){
        List<Vote> voters = article.getVoters();

        //linear search RIP server
        for(int i = 0; i < voters.size(); i ++){
            if(voters.get(i).getUserId() == userId){
                return i;
            }
        }
        return -1;
    }

    /**
     * removes any duplicate articles from an arraylist of articles
     * @param articles is the list of articles to remove from
     */
    public ArrayList<Article> removeDuplicates(ArrayList<Article> articles){
        ArrayList<Long> ids = new ArrayList<Long>();

        ArrayList<Article> returnList = new ArrayList<Article>();

        for(int i = 0; i < articles.size(); i ++){
            if(!ids.contains(articles.get(i).getId())){
                ids.add(articles.get(i).getId());
                returnList.add(articles.get(i));
            }
        }
        return returnList;
    }
}
