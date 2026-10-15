package me.rain.kernel;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

public final class PermissionSettingsFragment extends Fragment {
    private PermissionItemView networkItem;
    private PermissionItemView rootItem;
    private PermissionItemView installedAppsItem;
    private PermissionItemView lsposedItem;
    private final Handler mainHandler = new Handler(Looper.getMainLooper());
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
        lsposedItem = view.findViewById(R.id.lsp);
        networkItem.setItemTitle(R.string.provision_permission_internet);
        rootItem.setItemTitle(R.string.provision_permission_root);
        installedAppsItem.setItemTitle(R.string.provision_permission_installed_apps);
        lsposedItem.setItemTitle(R.string.provision_permission_lsp);
        rootItem.setChecked(false);
        installedAppsItem.setChecked(false);
        lsposedItem.setChecked(false);
        networkItem.setChecked(isNetworkValidated());
    }

    private boolean isNetworkValidated() {
        if (!isAdded()) return false;
        ConnectivityManager manager = (ConnectivityManager)
                requireContext().getSystemService(Context.CONNECTIVITY_SERVICE);
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
            } catch (RuntimeException error) {
                callback = null;
            }
        }
        refresh();
    }

    private void refresh() {
        mainHandler.post(() -> {
            if (isAdded() && networkItem != null) networkItem.setChecked(isNetworkValidated());
        });
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
        networkItem = null;
        rootItem = null;
        installedAppsItem = null;
        lsposedItem = null;
        super.onDestroyView();
    }
}
