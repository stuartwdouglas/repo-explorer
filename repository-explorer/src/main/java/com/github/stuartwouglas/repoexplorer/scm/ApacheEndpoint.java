package com.github.stuartwouglas.repoexplorer.scm;

import com.github.stuartwouglas.repoexplorer.model.Repository;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;

import javax.transaction.Transactional;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Path("load-apache-repos")
public class ApacheEndpoint {

    @GET
    @Transactional
    public void loadApacheRepos() {
        try (CloseableHttpClient c = HttpClientBuilder.create().build()) {
            var result = c.execute(new HttpGet("https://gitbox.apache.org/repos/asf"));
            String body = new String(result.getEntity().getContent().readAllBytes(), StandardCharsets.UTF_8);
            Matcher matcher = Pattern.compile("/repos/asf[^?]*?\\.git").matcher(body);
            while (matcher.find()) {
                Repository r = new Repository();
                r.uri = "https://gitbox.apache.org" + matcher.group();
                r.name = matcher.group();
                r.persistAndFlush();
            }

        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

}
