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
                                              "query=" + query + "&fmt=json",
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

                runLater(() -> {
                    tagDetails.clear();

                    if (!recordings.isEmpty()) {
                        for (int i = 0; i < recordings.length(); i++) {
                            JSONObject recording = recordings.getJSONObject(i);
                            String title = recording.getString("title");
                            JSONArray releases = recording.getJSONArray("releases");
                            JSONObject firstRelease = releases.getJSONObject(0);
                            String album = "";
                            if (firstRelease != null) {
                                album = firstRelease.getString("title");
                            }
                            JSONArray firstArtist = recording.getJSONArray("artist-credit");
                            String artistName = "";
                            if (firstArtist != null) {
                                artistName = firstArtist.getJSONObject(0).getJSONObject("artist").getString("name");
                            }

                            System.out.println("Track Title: " + title);
                            System.out.println("Album: " + album);
                            System.out.println("Artist: " + artistName);

                            tagDetails.add(new TagDetail(title, album, artistName));
                        }

                        resultsListView.getSelectionModel().selectFirst();
                    } else {
                        System.out.println("No recordings found.");
                    }
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