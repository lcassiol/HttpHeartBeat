package com.lcassiol;

import com.google.gson.Gson;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.HttpClientBuilder;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.ProtocolException;
import java.net.URL;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

/**
 * Developed By Cássio Carvalho at 08-10-2018
 * Github: lcassiol
 */

public class Main {

    public static void main(String[] args) {
        System.out.println("Start HTTP Heart Beat");
        InputStream input = null;
        HashMap<String, Integer> blackList = new HashMap<>();
        Properties prop = new Properties();
        String filename = "config.properties";
        String discordUrls = "";
        String currentUrl = "";

        while(true) {
            try {

                //input = Main.class.getClassLoader().getResourceAsStream(filename);
                input = new FileInputStream(filename);
                prop.load(input);

                String urlsToPing = prop.getProperty("urls");
                discordUrls = prop.getProperty("discordUrls");

                if (input != null) {
                    try {
                        input.close();
                        prop.clear();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
                if(urlsToPing.isEmpty())
                    return;

                for (int i = 0; i < 300; i++) {
                    String[] urlsToMonitoring = urlsToPing.split(",");
                    for (String stringUrl : urlsToMonitoring) {
                        currentUrl = stringUrl;
                        URL url = new URL(stringUrl);
                        HttpURLConnection con = (HttpURLConnection) url.openConnection();

                        con.setConnectTimeout(7000);
                        con.setReadTimeout(7000);
                        con.setRequestMethod("GET");

                        int responseCode = con.getResponseCode();
                        con.disconnect();
                        if (responseCode != 200) {
                            if (blackList.containsKey(stringUrl)) {
                                int quantity = blackList.get(stringUrl);
                                quantity++;
                                blackList.put(stringUrl, quantity);
                                if (quantity < 48 && quantity % 12 == 0) {
                                    callDiscord(new Date() + "- Hey Guys, Just to Remember the " + stringUrl + " still returns the code: " + responseCode,discordUrls);
                                } else if (quantity == 108 && quantity % 12 == 0) {
                                    callDiscord(new Date() + "- Hey Guys, Just to Remember the " + stringUrl + " still returns the code: " + responseCode,discordUrls);
                                }
                            } else {
                                callDiscord(new Date() + "- Hey Guys, Maybe We have a little problem here... I just called " + stringUrl + " and my response code was: " + responseCode,discordUrls);
                                blackList.put(stringUrl, 0);
                            }
                        } else if (blackList.containsKey(stringUrl)) {
                            int quantity = blackList.get(stringUrl);
                            callDiscord(new Date() + "- The " + stringUrl + " is alive! It's a miracle! I've been trying to contact " + quantity + " times",discordUrls);
                            blackList.remove(stringUrl);
                        }
                    }

                    Thread.sleep(5000);
                }

            }catch (FileNotFoundException ex){
                callDiscord("Sorry, unable to find " + filename,discordUrls);
                System.out.println("Sorry, unable to find " + filename);
                return;
            }
            catch (IOException ex) {
                System.out.println("Eita rapai... " + ex.getMessage() + " usei a url: " + currentUrl);
                callDiscord("Eita rapai... " + ex.getMessage() + " usei a url: " + currentUrl,discordUrls);
                if(!ex.getMessage().contains("connect timed out") && !ex.getMessage().contains("Read timed out"))
                    return;
            } catch (InterruptedException e) {
                System.out.println("Eitcha :" + e.getMessage());
                return;
            }
        }
    }

    private static void callDiscord(String errorMsg, String urlBot) {
        String[] listUrlBot = urlBot.split(",");
        if(urlBot.isEmpty())
            return;
        for (String stringUrl : listUrlBot) {
            String postUrl = stringUrl;// put in your url
            Gson gson = new Gson();
            HttpClient   httpClient    = HttpClientBuilder.create().build();
            HttpPost     post          = new HttpPost(postUrl);
            try {
                DiscordContent discordContent = new DiscordContent();
                discordContent.setContent(errorMsg);
                StringEntity postingString = new StringEntity(gson.toJson(discordContent));//gson.tojson() converts your pojo to json
                post.setEntity(postingString);
                post.setHeader("Content-type", "application/json");

                HttpResponse  response = httpClient.execute(post);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}
