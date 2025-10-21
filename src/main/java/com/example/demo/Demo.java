package com.example.demo;

import org.json.JSONArray;
import org.json.JSONObject;
import org.springframework.web.bind.annotation.*;

import javax.net.ssl.HttpsURLConnection;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.Base64;

@RestController
@RequestMapping("/demo")
@CrossOrigin(origins = "http://127.0.0.1:5500")
public class Demo {

    private static final String CLIENT_ID = "122ea05192a3454099e15dcec1472267";
    private static final String CLIENT_SECRET = "105a6fea566e4c37997cb2b9d21c930c";
    private final BazaDeDateRepository repo;

    public Demo(BazaDeDateRepository repo) {
        this.repo = repo;
    }

    public static String getAccessToken(String clientId, String clientSecret) throws Exception {
        String url = "https://accounts.spotify.com/api/token";
        String authString = clientId + ":" + clientSecret;
        String encodedAuth = Base64.getEncoder().encodeToString(authString.getBytes(StandardCharsets.UTF_8));

        URL obj = new URL(url);
        HttpURLConnection con = (HttpURLConnection) obj.openConnection();

        con.setRequestMethod("POST");
        con.setDoOutput(true);
        con.setRequestProperty("Authorization", "Basic " + encodedAuth);
        con.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");

        String urlParameters = "grant_type=client_credentials";
        OutputStream os = con.getOutputStream();
        os.write(urlParameters.getBytes(StandardCharsets.UTF_8));
        os.flush();
        os.close();

        int responseCode = con.getResponseCode();
        if (responseCode == HttpsURLConnection.HTTP_OK) {
            BufferedReader in = new BufferedReader(new InputStreamReader(con.getInputStream()));
            StringBuilder response = new StringBuilder();
            String inputLine;
            while ((inputLine = in.readLine()) != null) {
                response.append(inputLine);
            }
            in.close();
            JSONObject jsonResponse = new JSONObject(response.toString());
            return jsonResponse.getString("access_token");
        } else {
            throw new Exception("Failed to get access token: HTTP error code: " + responseCode);
        }
    }

    @PostMapping("/adauga-artist/{spotifyId}")
    public String adaugaArtistInBaza(@PathVariable String spotifyId) throws Exception {
        String accessToken = getAccessToken(CLIENT_ID, CLIENT_SECRET);
        String url = "https://api.spotify.com/v1/artists/" + spotifyId;

        HttpURLConnection con = (HttpURLConnection) new URL(url).openConnection();
        con.setRequestProperty("Authorization", "Bearer " + accessToken);

        BufferedReader in = new BufferedReader(new InputStreamReader(con.getInputStream()));
        StringBuilder response = new StringBuilder();
        String inputLine;
        while ((inputLine = in.readLine()) != null) {
            response.append(inputLine);
        }
        in.close();

        JSONObject spotifyArtist = new JSONObject(response.toString());
        String nume = spotifyArtist.optString("name", "Necunoscut");
        String artistID = spotifyId;  // Save the actual artist Spotify ID
        int versiune = spotifyArtist.optInt("popularity", 0);

        BazaDeDate artist = new BazaDeDate(nume, artistID, versiune);
        BazaDeDate salvata = repo.save(artist);

        JSONObject raspuns = new JSONObject();
        raspuns.put("status", "ok");
        raspuns.put("id", salvata.getId());
        raspuns.put("nume", salvata.getNume());
        raspuns.put("mesaj", "Artist salvat din Spotify în baza de date.");

        return raspuns.toString();
    }

    @GetMapping("/top-songs/{artistName}")
    public String getTopByArtist(
            @PathVariable String artistName,
            @RequestParam(defaultValue = "5") int limit,
            @RequestParam(defaultValue = "songs") String type
    ) throws Exception {
        String accessToken = getAccessToken(CLIENT_ID, CLIENT_SECRET);

        // Step 1: Search for artist by name
        String searchUrl = "https://api.spotify.com/v1/search?q=" +
                java.net.URLEncoder.encode(artistName, StandardCharsets.UTF_8) + "&type=artist&limit=1";

        HttpURLConnection searchCon = (HttpURLConnection) new URL(searchUrl).openConnection();
        searchCon.setRequestProperty("Authorization", "Bearer " + accessToken);

        BufferedReader searchIn = new BufferedReader(new InputStreamReader(searchCon.getInputStream()));
        StringBuilder searchResponse = new StringBuilder();
        String input;
        while ((input = searchIn.readLine()) != null) {
            searchResponse.append(input);
        }
        searchIn.close();

        JSONObject searchResult = new JSONObject(searchResponse.toString());
        JSONArray items = searchResult.getJSONObject("artists").getJSONArray("items");

        if (items.length() == 0) {
            JSONObject error = new JSONObject();
            error.put("error", "Artist not found");
            return error.toString();
        }

        String artistId = items.getJSONObject(0).getString("id");

        JSONObject result = new JSONObject();
        result.put("artist", items.getJSONObject(0).getString("name"));

        if ("albums".equalsIgnoreCase(type)) {
            // Use artist albums endpoint
            String albumsUrl = "https://api.spotify.com/v1/artists/" + artistId + "/albums?market=US&limit=" + limit;
            HttpURLConnection albumsCon = (HttpURLConnection) new URL(albumsUrl).openConnection();
            albumsCon.setRequestProperty("Authorization", "Bearer " + accessToken);

            BufferedReader albumsReader = new BufferedReader(new InputStreamReader(albumsCon.getInputStream()));
            StringBuilder albumsResponse = new StringBuilder();
            while ((input = albumsReader.readLine()) != null) {
                albumsResponse.append(input);
            }
            albumsReader.close();

            JSONObject albumsJson = new JSONObject(albumsResponse.toString());
            JSONArray albums = albumsJson.getJSONArray("items");

            JSONArray topAlbums = new JSONArray();
            for (int i = 0; i < Math.min(limit, albums.length()); i++) {
                JSONObject album = albums.getJSONObject(i);
                JSONObject obj = new JSONObject();
                obj.put("album_name", album.getString("name"));
                obj.put("release_date", album.optString("release_date", "N/A"));
                obj.put("spotify_url", album.getJSONObject("external_urls").getString("spotify"));
                JSONArray images = album.optJSONArray("images");
                if (images != null && images.length() > 0)
                    obj.put("image_url", images.getJSONObject(0).getString("url"));
                topAlbums.put(obj);
            }
            result.put("top_albums", topAlbums);
        } else {
            // Default: top tracks
            String topTracksUrl = "https://api.spotify.com/v1/artists/" + artistId + "/top-tracks?market=US";
            HttpURLConnection topCon = (HttpURLConnection) new URL(topTracksUrl).openConnection();
            topCon.setRequestProperty("Authorization", "Bearer " + accessToken);

            BufferedReader trackReader = new BufferedReader(new InputStreamReader(topCon.getInputStream()));
            StringBuilder trackResponse = new StringBuilder();
            while ((input = trackReader.readLine()) != null) {
                trackResponse.append(input);
            }
            trackReader.close();

            JSONObject trackJson = new JSONObject(trackResponse.toString());
            JSONArray tracks = trackJson.getJSONArray("tracks");

            JSONArray topTracks = new JSONArray();
            for (int i = 0; i < Math.min(limit, tracks.length()); i++) {
                JSONObject track = tracks.getJSONObject(i);
                JSONObject song = new JSONObject();
                song.put("title", track.getString("name"));
                song.put("album", track.getJSONObject("album").getString("name"));
                song.put("preview_url", track.optString("preview_url", "N/A"));
                song.put("spotify_url", track.getJSONObject("external_urls").getString("spotify"));
                JSONArray images = track.getJSONObject("album").optJSONArray("images");
                if (images != null && images.length() > 0)
                    song.put("image_url", images.getJSONObject(0).getString("url"));
                topTracks.put(song);
            }
            result.put("top_tracks", topTracks);
        }

        return result.toString(2); // formatted JSON output
    }

}
