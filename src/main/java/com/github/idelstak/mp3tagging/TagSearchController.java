package com.github.idelstak.mp3tagging;

import com.dlsc.gemsfx.*;
import javafx.beans.property.*;
import javafx.collections.*;
import javafx.concurrent.*;
import javafx.fxml.*;
import javafx.scene.control.*;
import org.json.*;
import org.slf4j.*;

import java.io.*;
import java.net.*;
import java.util.*;

import static javafx.application.Platform.*;

public class TagSearchController extends FxmlController {

    private static final Logger LOG = LoggerFactory.getLogger(TagSearchController.class);
    private final ObjectProperty<SearchQuery> searchQuery;
    private final ObservableList<TagDetail> tagDetails;
    @FXML
    private ProgressIndicator progressIndicator;
    @FXML
    private Label trackNumberLabel;
    @FXML
    private AvatarView coverArtView;
    @FXML
    private Label yearLabel;
    @FXML
    private Label artistLabel;
    @FXML
    private Label genreLabel;
    @FXML
    private ListView<TagDetail> resultsListView;
    @FXML
    private SplitPane detailsPane;
    @FXML
    private Label albumLabel;
    @FXML
    private Label trackTitleLabel;

    public TagSearchController() {
        searchQuery = new SimpleObjectProperty<>();
        tagDetails = FXCollections.observableArrayList();
    }

    @Override
    protected void initialize() {
        detailsPane.visibleProperty().bind(progressIndicator.visibleProperty().not());
        resultsListView.setCellFactory(_ -> new ListCell<>() {
            @Override
            protected void updateItem(TagDetail item, boolean empty) {
                super.updateItem(item, empty);

                if (empty || item == null) {
                    setText(null);
                } else {
                    setText(item.title());
                }
            }
        });
        MultipleSelectionModel<TagDetail> selectionModel = resultsListView.getSelectionModel();
        selectionModel.selectedItemProperty().addListener((_, _, detail) -> {
            if (detail == null) {
                return;
            }

            runLater(() -> {
                trackTitleLabel.setText(detail.title());
                artistLabel.setText(detail.artistName());
                albumLabel.setText(detail.album());
                yearLabel.setText(detail.releaseDate());
                genreLabel.setText(detail.genres());
            });
        });
        resultsListView.setItems(tagDetails);

        searchQuery.addListener((_, _, searchQuery) -> {
            if (searchQuery == null) {
                return;
            }

            Service<JSONObject> searchService = new Service<>() {
                @Override
                protected Task<JSONObject> createTask() {
                    return new Task<>() {
                        @Override
                        protected JSONObject call() throws Exception {
                            updateProgress(-1, -1);

                            URL url = composeUrl(searchQuery);
                            LOG.info("Opening setupConnection to: {}", url);

                            JSONObject response = fetchResponse(url);
                            updateProgress(1, 1);

                            return response;
                        }

                        private static URL composeUrl(SearchQuery searchQuery) throws URISyntaxException,
                                MalformedURLException {
                            String artist = fastEncode(searchQuery.artist());
                            String track = fastEncode(searchQuery.title());
                            String query = String.format("artist:\"%s\" OR recording:\"%s\"", artist, track);
                            URI uri = new URI("https",
                                              "musicbrainz.org",
                                              "/ws/2/recording",
                                              "query=" + query + "&limit=100&fmt=json",
                                              null);
                            return uri.toURL();
                        }
                    };
                }
            };

            progressIndicator.progressProperty().bind(searchService.progressProperty());
            progressIndicator.visibleProperty().bind(searchService.runningProperty());

            searchService.setOnFailed(event -> {
                LOG.error("Failed to search for tags", event.getSource().getException());
            });
            searchService.setOnSucceeded(event -> {
                JSONObject result = (JSONObject) event.getSource().getValue();
                JSONArray recordings = result.getJSONArray("recordings");
                List<TagDetail> tagDs = new ArrayList<>();

                if (recordings.isEmpty()) {
                    System.out.println("No recordings found.");
                } else {
                    for (int i = 0; i < recordings.length(); i++) {
                        System.out.println("recording #" + i + "====================");
                        JSONObject recording = recordings.getJSONObject(i);

                        String recordingId = recording.getString("id");
                        System.out.println("recordingId = " + recordingId);

                        String recordingTitle = recording.getString("title");
                        System.out.println("recordingTitle = " + recordingTitle);

                        String releaseDate = "";
                        try {
                            releaseDate = recording.getString("first-release-date");
                        } catch (JSONException e) {
                            LOG.debug("", e);
                        }
                        System.out.println("releaseDate = " + releaseDate);

                        JSONArray artistCredits = recording.getJSONArray("artist-credit");
                        StringJoiner artistNames = new StringJoiner(", ");
                        if (!artistCredits.isEmpty()) {
                            for (int j = 0; j < artistCredits.length(); j++) {
                                JSONObject artistCredit = artistCredits.getJSONObject(j);
                                String artistCreditName = artistCredit.getString("name");
                                System.out.println("artistCreditName = " + artistCreditName);

                                JSONObject artist = artistCredit.getJSONObject("artist");
                                String artistName = artist.getString("name");
                                artistName = (artistName == null || artistName.isBlank())
                                             ? artistCreditName
                                             : artistName;
                                artistNames.add(artistName);
                                System.out.printf("artistName #%d = %s%n", j, artistName);
                            }
                        }

                        JSONArray releases = null;
                        try {
                            releases = recording.getJSONArray("releases");
                        } catch (JSONException e) {
                            LOG.debug("", e);
                        }
                        String album = "";
                        if (releases != null && !releases.isEmpty()) {
                            JSONObject firstRelease = releases.getJSONObject(0);
                            String mbid = firstRelease.getString("id");
                            System.out.println("mbid = " + mbid);

                            String coverArtUrl = "http://coverartarchive.org/release/" + mbid;
                            System.out.println("coverArtUrl = " + coverArtUrl);

                            album = firstRelease.getString("title");
                            System.out.println("album = " + album);
                        }

                        JSONArray genres = null;
                        try {
                            genres = recording.getJSONArray("tags");
                        } catch (JSONException e) {
                            LOG.debug("Tags section not found", e);
                        }

                        StringJoiner genreTags = new StringJoiner(", ");
                        if (genres != null && genres.isEmpty()) {
                            for (int j = 0; j < genres.length(); j++) {
                                JSONObject genre = genres.getJSONObject(j);
                                String genreName = genre.getString("name");
                                System.out.println("genreName = " + genreName);
                                genreTags.add(genreName);
                            }
                        }

                        tagDs.add(new TagDetail(recordingTitle,
                                                album,
                                                artistNames.toString(),
                                                releaseDate,
                                                genreTags.toString()));
                    }
                }

                runLater(() -> {
                    tagDetails.setAll(tagDs);

                    resultsListView.getSelectionModel().selectFirst();
                });
            });

            searchService.start();

        });
    }

    private static JSONObject fetchResponse(URL url) throws IOException {
        HttpURLConnection connection = setupConnection(url);

        String inputLine;
        StringBuilder response = new StringBuilder();

        try (InputStream in = connection.getInputStream();
             InputStreamReader reader = new InputStreamReader(in);
             BufferedReader buffered = new BufferedReader(reader)) {
            while ((inputLine = buffered.readLine()) != null) {
                response.append(inputLine);
            }
        }

        return new JSONObject(response.toString());
    }

    private static String fastEncode(String text) {
        return text.replaceAll("[^A-Za-z0-9]", " ");
    }

    private static HttpURLConnection setupConnection(URL url) throws IOException {
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setRequestMethod("GET");
        connection.setRequestProperty("User-Agent", """
                                                    Mozilla/5.0 (Windows NT 10.0; Win64; x64) \
                                                    AppleWebKit/537.36 (KHTML, like Gecko) \
                                                    Chrome/91.0.4472.124 \
                                                    Safari/537.36""");
        return connection;
    }

    void setSearchQuery(SearchQuery searchQuery) {
        this.searchQuery.set(searchQuery);
    }

}