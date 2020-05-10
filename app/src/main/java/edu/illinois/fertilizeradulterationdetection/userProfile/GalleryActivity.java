package edu.illinois.fertilizeradulterationdetection.userProfile;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.content.SharedPreferences;
import android.os.Bundle;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Map;

import edu.illinois.fertilizeradulterationdetection.R;
import edu.illinois.fertilizeradulterationdetection.entity.Store;


public class GalleryActivity extends AppCompatActivity {
    private ArrayList<Store> stores;

    private RecyclerView mRecyclerView;
    private GalleryAdapter mAdapter;
    private RecyclerView.LayoutManager mLayoutManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        SharedPreferences sharedPreferences = getSharedPreferences("fertilizer_test_data", MODE_PRIVATE);
        Gson gson = new Gson();
        Type storeMapType = new TypeToken<Map<String, Store>>() {}.getType();
        Map<String, Store> storeMap = gson.fromJson(sharedPreferences.toString(), storeMapType);
        for(Map.Entry<String, Store> tmp : storeMap.entrySet()){
            stores.add(tmp.getValue());
        }

        buildRecyclerView();
    }

    public void buildRecyclerView() {
        mRecyclerView = findViewById(R.id.recycler_view);
        mRecyclerView.setHasFixedSize(true);
        mLayoutManager = new LinearLayoutManager(this);
        mAdapter = new GalleryAdapter(stores);

        mRecyclerView.setLayoutManager(mLayoutManager);
        mRecyclerView.setAdapter(mAdapter);

        mAdapter.setOnItemClickListener(new GalleryAdapter.OnItemClickListener() {
            @Override
            public void onItemClick(int position) {
                changeItem(position, "Clicked");
            }

            @Override
            public void onDeleteClick(int position) {
            }
        });
    }

    public void changeItem(int position, String text) {
        mAdapter.notifyItemChanged(position);
    }


}
