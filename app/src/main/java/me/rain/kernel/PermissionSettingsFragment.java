package me.rain.kernel;

import android.Manifest;
import android.app.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.provider.Settings;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import java.nio.charset.StandardCharsets;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public final class PermissionSettingsFragment extends Fragment {
    private PermissionItemView networkItem;
    private PermissionItemView rootItem;
    private PermissionItemView installedAppsItem;
    private PermissionItemView allFilesItem;
    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    private final ExecutorService rootWorker = Executors.newSingleThreadExecutor();
    private boolean rootCheckBusy;
    private ConnectivityManager connectivity;
    private ConnectivityManager.NetworkCallback callback;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container, @Nullable Bundle savedState) {
        return inflater.inflate(R.layout.provision_permission_layout, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle state) {
        super.onViewCreated(view, state);
        networkItem = view.findViewById(R.id.network);
        rootItem = view.findViewById(R.id.root);
        installedAppsItem = view.findViewById(R.id.installed_apps);
        allFilesItem = view.findViewById(R.id.all_files);

        networkItem.setItemTitle(R.string.provision_permission_internet);
        rootItem.setItemTitle(R.string.provision_permission_root);
        installedAppsItem.setItemTitle(R.string.provision_permission_installed_apps);
        allFilesItem.setItemTitle(R.string.provision_permission_all_files);
        rootItem.setChecked(false);
        networkItem.setOnClickListener(v -> {
            boolean granted = isNetworkValidated();
            networkItem.setChecked(granted);
            inform(granted ? R.string.permission_checked : R.string.permission_not_available);
        });
        rootItem.setOnClickListener(v -> checkRootPermission());
        installedAppsItem.setOnClickListener(v -> {
            boolean granted = isInstalledAppsVisible();
            installedAppsItem.setChecked(granted);
            inform(granted ? R.string.permission_checked : R.string.permission_apps_in_manifest);
        });
        allFilesItem.setOnClickListener(v -> requestAllFilesAccess());
        refresh();
    }

    private boolean isNetworkValidated() {
        Context context = getContext();
        if (context == null) return false;
        ConnectivityManager manager = (ConnectivityManager)
                context.getSystemService(Context.CONNECTIVITY_SERVICE);
        if (manager == null) return false;
        try {
            Network active = manager.getActiveNetwork();
            if (active == null) return false;
            NetworkCapabilities capabilities = manager.getNetworkCapabilities(active);
            return capabilities != null &&
                    capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED);
        } catch (SecurityException ignored) {
            return false;
        }
    }

    private boolean isInstalledAppsVisible() {
        Context context = getContext();
        return context != null && context.checkSelfPermission(
                Manifest.permission.QUERY_ALL_PACKAGES) == PackageManager.PERMISSION_GRANTED;
    }

    private void requestAllFilesAccess() {
        if (Environment.isExternalStorageManager()) {
            allFilesItem.setChecked(true);
            inform(R.string.permission_checked);
            return;
        }
        Context context = getContext();
        if (context == null) return;
        Intent appIntent = new Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION,
                Uri.parse("package:" + context.getPackageName()));
        try {
            startActivity(appIntent);
        } catch (ActivityNotFoundException | SecurityException error) {
            try {
                startActivity(new Intent(Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION));
            } catch (ActivityNotFoundException | SecurityException fallbackError) {
                inform(R.string.permission_files_settings_failed);
            }
        }
    }

    private void checkRootPermission() {
        if (rootWorker.isShutdown() || rootCheckBusy) return;
        rootCheckBusy = true;
        inform(R.string.permission_root_check_pending);
        rootWorker.execute(() -> {
            boolean granted = false;
            Process process = null;
            try {
                process = new ProcessBuilder("su", "-c", "id").redirectErrorStream(true).start();
                if (process.waitFor(6, TimeUnit.SECONDS) && process.exitValue() == 0) {
                    String output = new String(process.getInputStream().readAllBytes(),
                            StandardCharsets.UTF_8);
                    granted = output.contains("uid=0");
                }
            } catch (Exception exception) {
                if (exception instanceof InterruptedException) Thread.currentThread().interrupt();
            } finally {
                if (process != null && process.isAlive()) process.destroyForcibly();
            }
            final boolean result = granted;
            mainHandler.post(() -> {
                rootCheckBusy = false;
                if (!isAdded() || rootItem == null) return;
                rootItem.setChecked(result);
                inform(result ? R.string.permission_checked : R.string.permission_root_check_failed);
            });
        });
    }

    private void inform(int resource) {
        Context context = getContext();
        if (context != null) Toast.makeText(context, resource, Toast.LENGTH_SHORT).show();
    }

    private void refresh() {
        mainHandler.post(() -> {
            if (!isAdded() || networkItem == null || allFilesItem == null || installedAppsItem == null) return;
            networkItem.setChecked(isNetworkValidated());
            installedAppsItem.setChecked(isInstalledAppsVisible());
            allFilesItem.setChecked(Environment.isExternalStorageManager());
        });
    }

    @Override
    public void onStart() {
        super.onStart();
        connectivity = (ConnectivityManager)
                requireContext().getSystemService(Context.CONNECTIVITY_SERVICE);
        if (connectivity != null) {
            callback = new ConnectivityManager.NetworkCallback() {
                @Override public void onAvailable(@NonNull Network network) { refresh(); }
                @Override public void onLost(@NonNull Network network) { refresh(); }
                @Override public void onCapabilitiesChanged(@NonNull Network network,
                                      @NonNull NetworkCapabilities capabilities) { refresh(); }
            };
            try {
                connectivity.registerDefaultNetworkCallback(callback);
            } catch (RuntimeException exception) {
                callback = null;
            }
        }
        refresh();
    }

    @Override
    public void onResume() {
        super.onResume();
        refresh();
    }

    @Override
    public void onStop() {
        if (connectivity != null && callback != null) {
            try { connectivity.unregisterNetworkCallback(callback); }
            catch (IllegalArgumentException ignored) { }
            callback = null;
        }
        super.onStop();
    }

    @Override
    public void onDestroyView() {
        mainHandler.removeCallbacksAndMessages(null);
        rootCheckBusy = false;
        networkItem = null;
        rootItem = null;
        installedAppsItem = null;
        allFilesItem = null;
        super.onDestroyView();
    }

    @Override
    public void onDestroy() {
        rootWorker.shutdownNow();
        super.onDestroy();
    }
}
