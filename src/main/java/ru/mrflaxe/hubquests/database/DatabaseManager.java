package ru.mrflaxe.hubquests.database;

import java.io.IOException;
import java.sql.SQLException;
import java.util.List;
import java.util.concurrent.CompletableFuture;

import org.bukkit.Location;
import org.bukkit.plugin.Plugin;

import com.j256.ormlite.dao.Dao;
import com.j256.ormlite.dao.DaoManager;
import com.j256.ormlite.support.ConnectionSource;

import ru.mrflaxe.hubquests.database.model.ProfileModel;
import ru.mrflaxe.hubquests.database.model.SecretModel;
import ru.soknight.lib.database.Database;
import ru.soknight.lib.executable.quiet.AbstractQuietExecutor;

public class DatabaseManager extends AbstractQuietExecutor {
    
    private final ConnectionSource connection;
    private final Dao<ProfileModel, String> profileDao;
    private final Dao<SecretModel, String> secretDao;
    
    public DatabaseManager(Database database, Plugin plugin) throws SQLException {
        this.connection = database.establishConnection();
        this.profileDao = DaoManager.createDao(connection, ProfileModel.class);
        this.secretDao = DaoManager.createDao(connection, SecretModel.class);
        
        super.useDatabaseThrowableHandler(plugin);
        super.useCachedThreadPoolAsyncExecutor();
    }
    
    @Override
    public void shutdown() {
        try {
            if(connection != null)
                connection.close();
        } catch (IOException ignored) {}
    }
    
    
    public CompletableFuture<Void> saveProfile(ProfileModel profile) {
        return runQuietlyAsync(() -> profileDao.createOrUpdate(profile));
    }
    
    public CompletableFuture<Void> removeProfile(ProfileModel profile) {
        return runQuietlyAsync(() -> profileDao.delete(profile));
    }
    
    public CompletableFuture<ProfileModel> getProfile(String name) {
        return supplyQuietlyAsync(() -> profileDao.queryBuilder().where()
                    .eq("name", name)
                    .queryForFirst()
           );
    }
    
    public CompletableFuture<Boolean> hasProfile(String name) {
        return supplyQuietlyAsync(() -> profileDao.queryBuilder().where()
                .eq("name", name)
                .countOf() != 0L
           );
    }
    
    public CompletableFuture<Void> saveSecret(SecretModel secret) {
        return runQuietlyAsync(() -> secretDao.createOrUpdate(secret));
    }
    
    public CompletableFuture<Void> removeSecret(SecretModel secret) {
        return runQuietlyAsync(() -> secretDao.delete(secret));
    }
    
    public CompletableFuture<List<SecretModel>> getAllSecrets() {
        return supplyQuietlyAsync(() -> secretDao.queryForAll());
    }
    
    public CompletableFuture<SecretModel> getSecret(int id) {
        return supplyQuietlyAsync(() -> secretDao.queryBuilder().where()
                .eq("id", id)
                .queryForFirst()
          );
    }
    
    public CompletableFuture<SecretModel> getSecretByLoc(Location loc) {
        return supplyQuietlyAsync(() -> secretDao.queryBuilder().where()
                .eq("x", loc.getX()).and()
                .eq("y", loc.getY()).and()
                .eq("z", loc.getZ()).and()
                .eq("world", loc.getWorld().getName())
                .queryForFirst()
        );
    }
    
    public CompletableFuture<Boolean> hasSecret(int id) {
        return supplyQuietlyAsync(() -> secretDao.queryBuilder().where()
                .eq("id", id)
                .countOf() != 0L
          );

    }
}
