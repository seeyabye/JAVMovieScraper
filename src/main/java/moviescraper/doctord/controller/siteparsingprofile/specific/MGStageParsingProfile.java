package moviescraper.doctord.controller.siteparsingprofile.specific;

import java.io.File;
import java.io.IOException;
import java.net.SocketTimeoutException;
import java.net.URI;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import moviescraper.doctord.controller.languagetranslation.Language;
import moviescraper.doctord.controller.languagetranslation.TranslateString;
import moviescraper.doctord.controller.siteparsingprofile.SiteParsingProfile;
import moviescraper.doctord.controller.siteparsingprofile.SiteParsingProfileJSON;
import moviescraper.doctord.model.SearchResult;
import moviescraper.doctord.model.dataitem.Actor;
import moviescraper.doctord.model.dataitem.Director;
import moviescraper.doctord.model.dataitem.Genre;
import moviescraper.doctord.model.dataitem.ID;
import moviescraper.doctord.model.dataitem.MPAARating;
import moviescraper.doctord.model.dataitem.OriginalTitle;
import moviescraper.doctord.model.dataitem.Outline;
import moviescraper.doctord.model.dataitem.Plot;
import moviescraper.doctord.model.dataitem.Rating;
import moviescraper.doctord.model.dataitem.ReleaseDate;
import moviescraper.doctord.model.dataitem.Set;
import moviescraper.doctord.model.dataitem.SortTitle;
import moviescraper.doctord.model.dataitem.Studio;
import moviescraper.doctord.model.dataitem.Tagline;
import moviescraper.doctord.model.dataitem.Thumb;
import moviescraper.doctord.model.dataitem.Title;
import moviescraper.doctord.model.dataitem.Top250;
import moviescraper.doctord.model.dataitem.Trailer;
import moviescraper.doctord.model.dataitem.Votes;
import moviescraper.doctord.model.dataitem.Year;
import moviescraper.doctord.model.preferences.MoviescraperPreferences;

import org.apache.commons.codec.net.URLCodec;
import org.apache.commons.lang3.StringEscapeUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.text.WordUtils;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

public class MGStageParsingProfile extends SiteParsingProfile implements SpecificProfile {

	final static double mgstageMaxRating = 5.00;
	private final boolean doGoogleTranslation;
	private boolean scrapeTrailers;

	@Override
	public List<ScraperGroupName> getScraperGroupNames() {
		if (groupNames == null)
			groupNames = Arrays.asList(ScraperGroupName.JAV_CENSORED_SCRAPER_GROUP);
		return groupNames;
	}

	public MGStageParsingProfile() {
		super();
		doGoogleTranslation = (scrapingLanguage == Language.ENGLISH);
		scrapeTrailers = true;
	}

	public MGStageParsingProfile(final Document document) {
		super(document);
		doGoogleTranslation = (scrapingLanguage == Language.ENGLISH);
	}

	/**
	 * Default constructor does not define a document, so be careful not to call
	 * scrape methods without initializing the document first some other way.
	 * This constructor is mostly used for calling createSearchString() and
	 * getSearchResults()
	 */
	public MGStageParsingProfile(final boolean doGoogleTranslation) {
		super();
		this.doGoogleTranslation = doGoogleTranslation;
		if (this.doGoogleTranslation == false)
			setScrapingLanguage(Language.JAPANESE);
		scrapeTrailers = true;
	}

	public MGStageParsingProfile(final boolean doGoogleTranslation, final boolean scrapeTrailers) {
		super();
		this.doGoogleTranslation = doGoogleTranslation;
		if (this.doGoogleTranslation == false)
			setScrapingLanguage(Language.JAPANESE);
		this.scrapeTrailers = scrapeTrailers;
	}

	public MGStageParsingProfile(final Document document, final boolean doGoogleTranslation) {
		super(document);
		this.doGoogleTranslation = doGoogleTranslation;
		if (this.doGoogleTranslation == false)
			setScrapingLanguage(Language.JAPANESE);
	}

	@Override
	public Title scrapeTitle() {
		final Element titleElement = document.select(".tag").first();
		// run a google translate on the japanese title
		if (doGoogleTranslation) {
			return new Title(TranslateString.translateStringJapaneseToEnglish(titleElement.text()));
		} else {
			return new Title(titleElement.text());
		}
	}

	@Override
	public OriginalTitle scrapeOriginalTitle() {
		final Element titleElement = document.select(".tag").first();
		// leave the original title as the japanese title
		return new OriginalTitle(titleElement.text());
	}

	@Override
	public SortTitle scrapeSortTitle() {
		// we don't need any special sort title - that's usually something the
		// user provides
		return SortTitle.BLANK_SORTTITLE;
	}

	@Override
	public Set scrapeSet() {
		final Element setElement = document.select("table.mg-b20 tr td a[href*=article=series/id=]").first();
		if (setElement == null)
			return Set.BLANK_SET;
		else if (doGoogleTranslation) {
			return new Set(TranslateString.translateStringJapaneseToEnglish(setElement.text()));
		} else
			return new Set(setElement.text());
	}

	@Override
	public Rating scrapeRating() {
		Element ratingElement = document.select(".review").first();
		if (ratingElement != null)
			return new Rating(mgstageMaxRating, ratingElement.text().replaceAll("\\s+\\(\\d+ 件\\)\\s.*", ""));
		else
			return Rating.BLANK_RATING;
	}

	@Override
	public Year scrapeYear() {
		return scrapeReleaseDate().getYear();
	}

	@Override
	public ReleaseDate scrapeReleaseDate() {
		final Element releaseDateElement = document.select("th:contains(配信開始日：) + td").first();
		if (releaseDateElement != null) {
			String releaseDate = releaseDateElement.text();
			//we want to convert something like 2015/04/25 to 2015-04-25 
			releaseDate = StringUtils.replace(releaseDate, "/", "-");
			return new ReleaseDate(releaseDate);
		}
		return ReleaseDate.BLANK_RELEASEDATE;
	}

	@Override
	public Top250 scrapeTop250() {
		// This type of info doesn't exist on MGStage
		return Top250.BLANK_TOP250;
	}

	@Override
	public Votes scrapeVotes() {
		Element votesElement = document.select(".review").first();
		if (votesElement != null) {
			Pattern votePattern = Pattern.compile("\\((\\d+).*\\)");
			Matcher matchVote = votePattern.matcher(votesElement.text());
			if (matchVote.find())
				return new Votes(matchVote.group(1));
			else
				return Votes.BLANK_VOTES;
		}
		else
			return Votes.BLANK_VOTES;
	}

	@Override
	public Outline scrapeOutline() {
		// TODO Auto-generated method stub
		return Outline.BLANK_OUTLINE;
	}

	@Override
	public Plot scrapePlot() {

		//dvd mode
		Element plotElement = document.select("p.txt.introduction").first();
		if (plotElement == null || document.baseUri().contains("/digital/video")) {
			//video rental mode if it didnt find a match using above method
			plotElement = document.select("tbody .mg-b20.lh4").first();
		}
		if (doGoogleTranslation) {
			return new Plot(TranslateString.translateStringJapaneseToEnglish(plotElement.text()));
		} else
			return new Plot(plotElement.text());
	}

	@Override
	public Tagline scrapeTagline() {
		return Tagline.BLANK_TAGLINE;
	}

	@Override
	public moviescraper.doctord.model.dataitem.Runtime scrapeRuntime() {
		String runtime = "";
		final Element runtimeElement = document.select("th:contains(収録時間：) + td").first();
		if (runtimeElement != null) {
			// get rid of japanese word for minutes and just get the number
			runtime = runtimeElement.text().replaceAll("分", "");
		}
		return new moviescraper.doctord.model.dataitem.Runtime(runtime);

	}

	@Override
	public Trailer scrapeTrailer() {
		try {
			//we can return no trailers if scraping trailers is not enabled or the page we are scraping does not have a button to link to the trailer
            Element buttonElement;

			if (scrapeTrailers && (buttonElement = document.select("p.sample_movie_btn a").first()) != null) {
				System.out.println("There should be a trailer, searching now...");

                // First, scrape the maker's name
                // Then construct a url to obtain the sample video
                
                String makerName = document.select("th:contains(メーカー：) + td a[href]").first().attr("href").replace("/search/search.php?image_word_ids[]=", "");
                String codeName = document.select("th:contains(品番：) + td").first().text();
                String[] maker = codeName.split("-");
                final String potentialTrailerURL = String.format("https://sample.mgstage.com/sample/%1$s/%2$s/%3$s/%4$s_sample.mp4", makerName, maker[0].toLowerCase(), maker[1], codeName);
                // src="https://sample.mgstage.com/sample/shirouto/siro/3334/SIRO-3334_sample.mp4"

                if (SiteParsingProfile.fileExistsAtURL(potentialTrailerURL)) {
                    System.out.println("Trailer existed at: " + potentialTrailerURL);
                    return new Trailer(potentialTrailerURL);
                }

				System.err.println("I expected to find a trailer and did not at " + document.location());
			}
		} catch (final Exception e) {
			e.printStackTrace();
		}

		return Trailer.BLANK_TRAILER;
	}

	@Override
	public Thumb[] scrapePosters() {
        //don't crop the cover for certain makers as it is a website release and does not have dvd art
        if(document.select("ul.Bread_crumb li:contains(配信専用動画)").first() != null)
			return scrapePostersAndFanart(false, false);
		else
			return scrapePostersAndFanart(true, false);
	}

	/**
	 * Helper method for scrapePoster() and scapeFanart since this code is
	 * virtually identical
	 * 
	 * @param doCrop
	 * - if true, will only get the front cover as the initial poster
	 * element; otherwise it uses the entire dvd case from DMM.co.jp
	 * @return Thumb[] containing all the scraped poster and extraart (if doCrop
	 * is true) or the cover and back in extraart (if doCrop is false)
	 */
	private Thumb[] scrapePostersAndFanart(final boolean doCrop, final boolean scrapingExtraFanart) {

		// the movie poster, on this site it usually has both front and back
		// cover joined in one image
        //final Element postersElement = document.select("a[name=package-image], div#sample-video img[src*=/pics.dmm.co.jp]").first();
        final Element postersElement = document.select("a#EnlargeImage").first();
		// the extra screenshots for this movie. It's just the thumbnail as the
		// actual url requires javascript to find.
		// We can do some string manipulation on the thumbnail URL to get the
		// full URL, however
		final Elements extraArtElementsSmallSize = document.select("dl li");

		final ArrayList<Thumb> posters = new ArrayList<>(1 + extraArtElementsSmallSize.size());
		String posterLink = postersElement.attr("abs:href");
		if (posterLink == null || posterLink.length() < 1)
			posterLink = postersElement.attr("abs:src");
		try {
			// for the poster, do a crop of the the right side of the dvd case image 
			//(which includes both cover art and back art)
			// so we only get the cover
			if (doCrop && !scrapingExtraFanart)
				//use javCropCoverRoutine version of the new Thumb constructor to handle the cropping
				posters.add(new Thumb(posterLink, true));
			else if (!scrapingExtraFanart)
				posters.add(new Thumb(posterLink));
		} catch (final IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		/* if (scrapingExtraFanart) {
			// maybe you're someone who doesn't want the movie poster as the cover.
			// Include the extra art in case
            // you want to use one of those
            
			for (final Element item : extraArtElementsSmallSize) {

				// We need to do some string manipulation and put a "jp" before the
				// last dash in the URL to get the full size picture
				final String extraArtLinkSmall = item.attr("abs:src");
				final int indexOfLastDash = extraArtLinkSmall.lastIndexOf('-');
				final String URLpath = extraArtLinkSmall.substring(0, indexOfLastDash) + "jp" + extraArtLinkSmall.substring(indexOfLastDash);
				try {
					if (Thumb.fileExistsAtUrl(URLpath))
						posters.add(new Thumb(URLpath));
				} catch (final IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
		} */
		return posters.toArray(new Thumb[0]);
	}

	@Override
	public Thumb[] scrapeFanart() {
		return scrapePostersAndFanart(false, false);
	}

	@Override
	public MPAARating scrapeMPAA() {
		return MPAARating.RATING_XXX;
	}

	@Override
	public ID scrapeID() {
		final Element idElement = document.select("th:contains(品番：) + td").first();
		if (idElement != null) {
			String idElementText = idElement.text();
			//idElementText = fixUpIDFormatting(idElementText);
			return new ID(idElementText);
		}
		//This page didn't have an ID, so just put in a empty one
		else
			return ID.BLANK_ID;
	}

	public static String fixUpIDFormatting(String idElementText) {
		//DMM sometimes has a letter and underscore then followed by numbers. numbers will be stripped in the next step, so let's strip out the underscore prefix part of the string
		if (idElementText.contains("_")) {
			idElementText = idElementText.substring(idElementText.indexOf('_') + 1);
		}

		//DMM sometimes includes numbers before the ID, so we're going to strip them out to use
		//the same convention that other sites use for the id number
		idElementText = idElementText.substring(StringUtils.indexOfAnyBut(idElementText, "0123456789"));
		//Dmm has everything in lowercase for this field; most sites use uppercase letters as that follows what shows on the cover so will uppercase the string
		//English locale used for uppercasing just in case user is in some region that messes with the logic of this code...
		idElementText = idElementText.toUpperCase(Locale.ENGLISH);
		//insert the dash between the text and number part
		final int firstNumberIndex = StringUtils.indexOfAny(idElementText, "0123456789");
		idElementText = idElementText.substring(0, firstNumberIndex) + "-" + idElementText.substring(firstNumberIndex);

		//remove extra zeros in case we get a 5 or 6 digit numerical part 
		//(For example ABC-00123 will become ABC-123)
		final Pattern patternID = Pattern.compile("([0-9]*\\D+)(\\d{5,6})");
		final Matcher matcher = patternID.matcher(idElementText);
		String groupOne = "";
		String groupTwo = "";
		while (matcher.find()) {
			groupOne = matcher.group(1);
			groupTwo = matcher.group(2);
		}
		if (groupOne.length() > 0 && groupTwo.length() > 0) {
			groupTwo = String.format("%03d", Integer.parseInt(groupTwo));
			return groupOne + groupTwo;
		}
		return idElementText;
	}

	@Override
	public ArrayList<Genre> scrapeGenres() {
		final Elements genreElements = document.select("div.detail_data tr th:contains(ジャンル) + td a[href*=/search]");
		final ArrayList<Genre> genres = new ArrayList<>(genreElements.size());
		for (final Element genreElement : genreElements) {
			// get the link so we can examine the id and do some sanity cleanup
			// and perhaps some better translation that what google has, if we
            // happen to know better
            
            // TODO Skip this for now
            genres.add(new Genre(genreElement.text()));

			/* final String href = genreElement.attr("abs:href");
			final String genreID = genreElement.attr("abs:href").substring(href.indexOf("id=") + 3, href.length() - 1);
			if (acceptGenreID(genreID)) {
				if (doGoogleTranslation == false) {
					genres.add(new Genre(genreElement.text()));
				} else {
					final String potentialBetterTranslation = betterGenreTranslation(genreElement.text(), genreID);

					// we didn't know of anything hand picked for genres, just use
					// google translate
					if (potentialBetterTranslation.equals("")) {
						genres.add(new Genre(TranslateString.translateStringJapaneseToEnglish(genreElement.text())));
					}
					// Cool, we got something we want to use instead for our genre,
					// let's use that
					else {
						genres.add(new Genre(potentialBetterTranslation));
					}
				}
			} */
		}
		// System.out.println("genres" + genreElements);
		return genres;
	}

	private String betterGenreTranslation(final String text, final String genreID) {
		String betterGenreTranslatedString = "";
		switch (genreID) {
			case "5001":
				betterGenreTranslatedString = "Creampie";
				break;
			case "5002":
				betterGenreTranslatedString = "Fellatio";
				break;
			case "1013":
				betterGenreTranslatedString = "Nurse";
				break;
			default:
				break;
		}

		return betterGenreTranslatedString;
	}

	private String betterActressTranslation(final String text, final String actressID) {
		String betterActressTranslatedString = "";
		switch (actressID) {
			case "17802":
				betterActressTranslatedString = "Tsubomi";
				break;
			case "27815":
				betterActressTranslatedString = "Sakura Aida";
				break;
			case "1014395":
				betterActressTranslatedString = "Yuria Ashina";
				break;
			case "1001819":
				betterActressTranslatedString = "Emiri Himeno";
				break;
			case "1006261":
				betterActressTranslatedString = "Uta Kohaku";
				break;
			case "101792":
				betterActressTranslatedString = "Nico Nohara";
				break;
			case "1015472":
				betterActressTranslatedString = "Tia";
				break;
			case "1016186":
				betterActressTranslatedString = "Yuko Shiraki";
				break;
			case "1009910":
				betterActressTranslatedString = "Hana Nonoka";
				break;
			case "1016458":
				betterActressTranslatedString = "Eve Hoshino";
				break;
			case "1019676":
				betterActressTranslatedString = "Rie Tachikawa";
				break;
			case "1017201":
				betterActressTranslatedString = "Meisa Chibana";
				break;
			case "1018387":
				betterActressTranslatedString = "Nami Itoshino";
				break;
			case "1014108":
				betterActressTranslatedString = "Juria Tachibana";
				break;
			case "1016575":
				betterActressTranslatedString = "Chika Kitano";
				break;
			case "24489":
				betterActressTranslatedString = "Chichi Asada";
				break;
			case "20631":
				betterActressTranslatedString = "Mitsuki An";
				break;
			default:
				break;

		}

		return betterActressTranslatedString;
	}

	// Return false on any genres we don't want scraping in. This can later be
	// something the user configures, but for now I'll use it
	// to get rid of weird stuff like DVD toaster
	// the genreID comes from the href to the genre keyword from DMM
	// Example: <a href="/mono/dvd/-/list/=/article=keyword/id=6004/">
	// The genre ID would be 6004 which is passed in as the String
	private boolean acceptGenreID(final String genreID) {
		switch (genreID) {
			case "6529": // "DVD Toaster" WTF is this? Nuke it!
				return false;
			case "6102": // "Sample Video" This is not a genre!
				return false;
			default:
				break;
		}
		return true;
	}

	@Override
	public ArrayList<Actor> scrapeActors() {
		// scrape all the actress IDs
        //final Elements actressIDElements = document.select("div.detail_left th:contains(出演：) + td a[href*=/search]");    
		//final ArrayList<Actor> actorList = new ArrayList<>(actressIDElements.size());		

		//Get actors that are just a "Name" and have no page of their own (common on some web releases)
		final Elements nameOnlyActors = document.select("div.detail_left th:contains(出演：) + td");
		final ArrayList<Actor> actorList = new ArrayList<>();	
		for (final Element currentNameOnlyActor : nameOnlyActors) {
			String actorName = currentNameOnlyActor.text().trim();
			//for some reason, they sometimes list the age of the person after their name, so let's get rid of that
			actorName = actorName.replaceFirst("\\([0-9]{2}\\)", "");
			if (doGoogleTranslation)
				actorName = TranslateString.translateJapanesePersonNameToRomaji(actorName);
			actorList.add(new Actor(actorName, "", null));
		}

		return actorList;
	}

	@Override
	public ArrayList<Director> scrapeDirectors() {
        //No Director exists.
		final ArrayList<Director> directors = new ArrayList<>();
		return directors;
	}

	@Override
	public Studio scrapeStudio() {
		final Element studioElement = document.select("div.detail_left th:contains(レーベル：) + td a[href*=/search]").first();
		if (studioElement != null) {
			if (doGoogleTranslation)
				return new Studio(TranslateString.translateStringJapaneseToEnglish(studioElement.text()));
			else
				return new Studio(studioElement.text());
		} else
			return Studio.BLANK_STUDIO;
	}

	@Override
	public String createSearchString(final File file) {
		scrapedMovieFile = file;
		final String fileNameNoExtension = findIDTagFromFile(file, isFirstWordOfFileIsID());
		//System.out.println("fileNameNoExtension in DMM: " + fileNameNoExtension);
		final URLCodec codec = new URLCodec();
		try {
			final String fileNameURLEncoded = codec.encode(fileNameNoExtension);
			//System.out.println("FileNameUrlencode = " + fileNameURLEncoded);
			return "https://www.mgstage.com/search/search.php?search_word=" + fileNameURLEncoded;
		} catch (final Exception e) {
			e.printStackTrace();
		}
		return null;
	}

	/**
	 * returns a String[] filled in with urls of each of the possible movies
	 * found on the page returned from createSearchString
	 * 
	 * @throws IOException
	 */
	@Override
	public SearchResult[] getSearchResults(final String searchString) throws IOException {
        // Pagination ignored for now
		boolean firstPageScraping = true;
		Document searchResultsPage = Jsoup.connect(searchString).cookie("adc", "1").timeout(CONNECTION_TIMEOUT_VALUE).get();
		System.out.print(searchResultsPage.body());
		Element nextPageLink = searchResultsPage.select("div.list-capt div.list-boxcaptside.list-boxpagenation ul li:not(.terminal) a").last();
		final ArrayList<SearchResult> searchResults = new ArrayList<>();
		final ArrayList<String> pagesVisited = new ArrayList<>();
		while (firstPageScraping || nextPageLink != null) {
			nextPageLink = searchResultsPage.select("div.list-capt div.list-boxcaptside.list-boxpagenation ul li:not(.terminal) a").last();
			final String currentPageURL = searchResultsPage.baseUri();
			String nextPageURL = "";
			if (nextPageLink != null)
				nextPageURL = nextPageLink.attr("abs:href");
			pagesVisited.add(currentPageURL);
			//I can probably combine this into one selector, but it wasn't working when I tried it,
			//so for now I'm making each its own variable and looping through and adding in all the elements seperately
			// final Elements dvdLinks = searchResultsPage.select("p.tmb a[href*=/mono/dvd/");
			// final Elements rentalElements = searchResultsPage.select("p.tmb a[href*=/rental/ppr/");
            // final Elements digitalElements = searchResultsPage.select("p.tmb a[href*=/digital/videoa/], p.tmb a[href*=/digital/videoc/]");
            final Elements resultLinks = searchResultsPage.select("div.search_list li a[href*=/product]");

			//get /product/product_detail
			for (int i = 0; i < resultLinks.size(); i++) {
				final String currentLink = resultLinks.get(i).attr("abs:href");
				final Element imageLinkElement = resultLinks.get(i).select("img").first();
				if (imageLinkElement != null) {
					final Thumb currentPosterThumbnail = new Thumb(imageLinkElement.attr("abs:src"));
					searchResults.add(new SearchResult(currentLink, "", currentPosterThumbnail));
				} else {
					searchResults.add(new SearchResult(currentLink));
				}
			}
			
			firstPageScraping = false;
			//get the next page of search results (if it exists) using the "next page" link, but only if we haven't visited that page before
			//TODO this is really not the cleanest way of doing this - I can probably find some way to make the selector not send me in a loop
			//of pages, but this will work for now
			if (nextPageLink != null && !pagesVisited.contains(nextPageURL))
				searchResultsPage = Jsoup.connect(nextPageURL).get();
			else
				break;

		}

		return searchResults.toArray(new SearchResult[searchResults.size()]);
	}

	public SearchResult[] getSearchResultsWithoutDVDLinks(final String mgstageSearchString) throws IOException {
		final SearchResult[] allSearchResult = getSearchResults(mgstageSearchString);
		final List<SearchResult> filteredSearchResults = new LinkedList<>();
		for (final SearchResult currentSR : allSearchResult) {
			System.out.println("current SR = " + currentSR.getUrlPath());
			if (!currentSR.getUrlPath().contains("/mono/dvd/"))
				filteredSearchResults.add(currentSR);
		}

		return filteredSearchResults.toArray(new SearchResult[filteredSearchResults.size()]);

	}

	@Override
	public Thumb[] scrapeExtraFanart() {
		if (super.isExtraFanartScrapingEnabled())
			return scrapePostersAndFanart(false, true);
		else
			return new Thumb[0];
	}

	@Override
	public String toString() {
		return "MGStage.com";
	}

	@Override
	public SiteParsingProfile newInstance() {
		final MoviescraperPreferences preferences = MoviescraperPreferences.getInstance();
		return new MGStageParsingProfile(!preferences.getScrapeInJapanese());
	}

	@Override
	public String getParserName() {
		return "MGStage.com";
	}

	@Override
	public Document downloadDocument(SearchResult searchResult) {
		try {
			if (searchResult.isJSONSearchResult())
				return SiteParsingProfileJSON.getDocument(searchResult.getUrlPath());
			else
				return Jsoup.connect(searchResult.getUrlPath()).cookie("adc", "1").userAgent("Mozilla").ignoreHttpErrors(true).timeout(CONNECTION_TIMEOUT_VALUE).get();
		} catch (IOException e) {
			e.printStackTrace();
		}
		return null;
	}
}
