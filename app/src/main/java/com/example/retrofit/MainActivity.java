package com.example.retrofit;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.MutableLiveData;

import android.content.Context;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

import retrofit2.Call;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;
import retrofit2.http.GET;
import retrofit2.http.Path;

public class MainActivity extends AppCompatActivity {

    public ListView repos;
    public Button search;
    public EditText username;

    private MutableLiveData<List<RepoItem>> reposData = new MutableLiveData<>();
    private final static Executor executor = Executors.newSingleThreadExecutor(); // change according to your requirements
    private final static Handler handler = new Handler(Looper.getMainLooper());
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        SimpleService.initalize();
        repos=findViewById(R.id.repos);
        search=findViewById(R.id.search);
        username=findViewById(R.id.username);
        search.setOnClickListener(this::search);
        reposData.observe(this, repoItems -> {
            if(repoItems == null){
                Toast.makeText(this, "User doesn't exist!", Toast.LENGTH_SHORT).show();
                return;
            }
            repos.setAdapter(new RepoAdapter(this, repoItems));
        });
    }

    public void search(View view) {
        runInBackground(() -> {
            List<RepoItem> newRepos = null;
            try {
                newRepos = SimpleService.getRepos(username.getText().toString());
            } catch (IOException e) {
                handler.post(() -> {
                    Toast.makeText(this, "Error during request", Toast.LENGTH_SHORT).show();
                });
                return;
            }
            List<RepoItem> finalNewRepos = newRepos;
            handler.post(() -> {
                reposData.postValue(finalNewRepos);
            });
        });
    }
    public static void runInBackground(Runnable runnable) {
        executor.execute(runnable);
    }
}

final class SimpleService {
    public static final String API_URL = "https://api.github.com";
    public static List<RepoItem> getRepos(String user) throws IOException {
        // Create a call instance for looking up Retrofit repos.
        Call<List<RepoItem>> call = github.repos(user);

            List<RepoItem> repos = null;
            repos = call.execute().body();
            return repos;
    }

    private static Retrofit retrofit;
    private static GitHub github;
    public interface GitHub {
        @GET("/users/{user}/repos")
        Call<List<RepoItem>> repos(@Path("user") String user);
    }

    public static void initalize() {
        // Create a very simple REST adapter which points the GitHub API.
        retrofit =
                new Retrofit.Builder()
                        .baseUrl(API_URL)
                        .addConverterFactory(GsonConverterFactory.create())
                        .build();

        // Create an instance of our GitHub API interface.
        github = retrofit.create(GitHub.class);

    }
}


class RepoItem {
    public final String name;

    RepoItem(String name) {
        this.name = name;
    }
    public String getName() {
        return name;
    }
}

class RepoAdapter extends ArrayAdapter<RepoItem>
{
    public RepoAdapter(Context context, List<RepoItem> weatherList) {
        super(context, 0, weatherList);
    }
    @NonNull
    @Override
    public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
        View itemView = convertView;
        if (itemView == null) {
            itemView = LayoutInflater.from(getContext()).inflate(R.layout.repo_item_view, parent, false);
        }
        RepoItem currentRepo = getItem(position);


        TextView cityNameTextView = itemView.findViewById(R.id.repoName);
        cityNameTextView.setText(currentRepo.getName());

        return itemView;
    }
}