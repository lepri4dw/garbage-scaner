package com.example.garbagescaner.fragments;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.garbagescaner.R;
import com.example.garbagescaner.adapters.ScanHistoryAdapter;
import com.example.garbagescaner.database.ScanHistoryManager;
import com.example.garbagescaner.maps.MapActivity;
import com.example.garbagescaner.models.ScanResult;

import java.util.List;

public class ProfileFragment extends Fragment implements ScanHistoryAdapter.OnHistoryItemClickListener {

    private RecyclerView recyclerView;
    private TextView emptyView;
    private ScanHistoryAdapter adapter;
    private ScanHistoryManager historyManager;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_profile, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // Инициализируем компоненты
        recyclerView = view.findViewById(R.id.recyclerViewHistory);
        emptyView = view.findViewById(R.id.emptyView);

        // Настраиваем RecyclerView
        recyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));

        // Инициализируем менеджер истории
        historyManager = new ScanHistoryManager(requireContext());

        // Загружаем историю при создании фрагмента
        loadHistory();
    }

    @Override
    public void onResume() {
        super.onResume();
        // Обновляем историю при возвращении к фрагменту
        loadHistory();
    }

    private void loadHistory() {
        List<ScanResult> history = historyManager.getAllScanResults();

        if (history.isEmpty()) {
            recyclerView.setVisibility(View.GONE);
            emptyView.setVisibility(View.VISIBLE);
        } else {
            recyclerView.setVisibility(View.VISIBLE);
            emptyView.setVisibility(View.GONE);

            adapter = new ScanHistoryAdapter(requireContext(), history, this);
            recyclerView.setAdapter(adapter);
        }
    }

    @Override
    public void onItemClick(ScanResult scanResult) {
        // При клике на элемент истории можем отобразить детали или выполнить другие действия
        // Например, можно открыть карту с пунктами приема для этого типа отходов
        Intent intent = new Intent(requireContext(), MapActivity.class);
        intent.putExtra("waste_type", scanResult.getWasteType());
        startActivity(intent);
    }
}