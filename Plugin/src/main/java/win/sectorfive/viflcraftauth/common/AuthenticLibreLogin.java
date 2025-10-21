/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package win.sectorfive.viflcraftauth.common;

import co.aikar.commands.CommandIssuer;
import co.aikar.commands.CommandManager;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import net.kyori.adventure.audience.Audience;
import org.bstats.charts.CustomChart;
import org.jetbrains.annotations.Nullable;
import win.sectorfive.viflcraftauth.api.BiHolder;
import win.sectorfive.viflcraftauth.api.LibreLoginPlugin;
import win.sectorfive.viflcraftauth.api.Logger;
import win.sectorfive.viflcraftauth.api.PlatformHandle;
import win.sectorfive.viflcraftauth.api.configuration.CorruptedConfigurationException;
import win.sectorfive.viflcraftauth.api.crypto.CryptoProvider;
import win.sectorfive.viflcraftauth.api.crypto.HashedPassword;
import win.sectorfive.viflcraftauth.api.database.*;
import win.sectorfive.viflcraftauth.api.database.connector.DatabaseConnector;
import win.sectorfive.viflcraftauth.api.database.connector.MySQLDatabaseConnector;
import win.sectorfive.viflcraftauth.api.database.connector.PostgreSQLDatabaseConnector;
import win.sectorfive.viflcraftauth.api.database.connector.SQLiteDatabaseConnector;
import win.sectorfive.viflcraftauth.api.integration.LimboIntegration;
import win.sectorfive.viflcraftauth.api.premium.PremiumException;
import win.sectorfive.viflcraftauth.api.premium.PremiumUser;
import win.sectorfive.viflcraftauth.api.server.ServerHandler;
import win.sectorfive.viflcraftauth.api.util.Release;
import win.sectorfive.viflcraftauth.api.util.SemanticVersion;
import win.sectorfive.viflcraftauth.api.util.ThrowableFunction;
import win.sectorfive.viflcraftauth.common.authorization.AuthenticAuthorizationProvider;
import win.sectorfive.viflcraftauth.common.command.CommandProvider;
import win.sectorfive.viflcraftauth.common.command.InvalidCommandArgument;
import win.sectorfive.viflcraftauth.common.config.HoconMessages;
import win.sectorfive.viflcraftauth.common.config.HoconPluginConfiguration;
import win.sectorfive.viflcraftauth.common.crypto.Argon2IDCryptoProvider;
import win.sectorfive.viflcraftauth.common.crypto.BCrypt2ACryptoProvider;
import win.sectorfive.viflcraftauth.common.crypto.LogITMessageDigestCryptoProvider;
import win.sectorfive.viflcraftauth.common.crypto.MessageDigestCryptoProvider;
import win.sectorfive.viflcraftauth.common.database.AuthenticDatabaseProvider;
import win.sectorfive.viflcraftauth.common.database.AuthenticUser;
import win.sectorfive.viflcraftauth.common.database.connector.AuthenticMySQLDatabaseConnector;
import win.sectorfive.viflcraftauth.common.database.connector.AuthenticPostgreSQLDatabaseConnector;
import win.sectorfive.viflcraftauth.common.database.connector.AuthenticSQLiteDatabaseConnector;
import win.sectorfive.viflcraftauth.common.database.connector.DatabaseConnectorRegistration;
import win.sectorfive.viflcraftauth.common.database.provider.LibreLoginMySQLDatabaseProvider;
import win.sectorfive.viflcraftauth.common.database.provider.LibreLoginPostgreSQLDatabaseProvider;
import win.sectorfive.viflcraftauth.common.database.provider.LibreLoginSQLiteDatabaseProvider;
import win.sectorfive.viflcraftauth.common.event.AuthenticEventProvider;
import win.sectorfive.viflcraftauth.common.image.AuthenticImageProjector;
import win.sectorfive.viflcraftauth.common.integration.FloodgateIntegration;
import win.sectorfive.viflcraftauth.common.integration.luckperms.LuckPermsIntegration;
import win.sectorfive.viflcraftauth.common.listener.LoginTryListener;
import win.sectorfive.viflcraftauth.common.log.Log4JFilter;
import win.sectorfive.viflcraftauth.common.log.SimpleLogFilter;
import win.sectorfive.viflcraftauth.common.mail.AuthenticEMailHandler;
// Migration classes removed - data migration feature removed
// import win.sectorfive.viflcraftauth.common.migrate.*;
import win.sectorfive.viflcraftauth.common.premium.AuthenticPremiumProvider;
import win.sectorfive.viflcraftauth.common.server.AuthenticServerHandler;
import win.sectorfive.viflcraftauth.common.util.CancellableTask;
import win.sectorfive.viflcraftauth.common.util.GeneralUtil;

import java.io.*;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.sql.Timestamp;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ForkJoinPool;

import static win.sectorfive.viflcraftauth.common.config.ConfigurationKeys.*;

public abstract class AuthenticLibreLogin<P, S> implements LibreLoginPlugin<P, S> {

    public static final Gson GSON = new Gson();
    public static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("dd. MM. yyyy HH:mm");
    public static final ExecutorService EXECUTOR;

    static {
        EXECUTOR = new ForkJoinPool(4);
    }

    private final Map<String, CryptoProvider> cryptoProviders;
    private final Map<String, ReadDatabaseProviderRegistration<?, ?, ?>> readProviders;
    private final Map<Class<?>, DatabaseConnectorRegistration<?, ?>> databaseConnectors;
    private final Multimap<P, CancellableTask> cancelOnExit;
    private final PlatformHandle<P, S> platformHandle;
    protected Logger logger;
    private AuthenticPremiumProvider premiumProvider;
    private AuthenticEventProvider<P, S> eventProvider;
    private AuthenticServerHandler<P, S> serverHandler;
    private AuthenticImageProjector<P, S> imageProjector;
    private FloodgateIntegration floodgateApi;
    private LuckPermsIntegration<P, S> luckpermsApi;
    private SemanticVersion version;
    private HoconPluginConfiguration configuration;
    private HoconMessages messages;
    private AuthenticAuthorizationProvider<P, S> authorizationProvider;
    private CommandProvider<P, S> commandProvider;
    private ReadWriteDatabaseProvider databaseProvider;
    private DatabaseConnector<?, ?> databaseConnector;
    private AuthenticEMailHandler eMailHandler;
    private LoginTryListener<P, S> loginTryListener;

    protected AuthenticLibreLogin() {
        cryptoProviders = new ConcurrentHashMap<>();
        readProviders = new ConcurrentHashMap<>();
        databaseConnectors = new ConcurrentHashMap<>();
        platformHandle = providePlatformHandle();
        cancelOnExit = HashMultimap.create();
    }

    public Map<Class<?>, DatabaseConnectorRegistration<?, ?>> getDatabaseConnectors() {
        return databaseConnectors;
    }

    @Override
    public <E extends Exception, C extends DatabaseConnector<E, ?>> void registerDatabaseConnector(Class<?> clazz, ThrowableFunction<String, C, E> factory, String id) {
        registerDatabaseConnector(new DatabaseConnectorRegistration<>(factory, null, id), clazz);
    }

    @Override
    public void registerReadProvider(ReadDatabaseProviderRegistration<?, ?, ?> registration) {
        readProviders.put(registration.id(), registration);
    }

    @Override
    public AuthenticEMailHandler getEmailHandler() {
        return eMailHandler;
    }

    @Nullable
    @Override
    public LimboIntegration<S> getLimboIntegration() {
        return null;
    }

    @Override
    public User createUser(UUID uuid, UUID premiumUUID, HashedPassword hashedPassword, String lastNickname, Timestamp joinDate, Timestamp lastSeen, String ip, Timestamp lastAuthentication, String lastServer, String email) {
        return new AuthenticUser(uuid, premiumUUID, hashedPassword, lastNickname, joinDate, lastSeen, lastAuthentication, lastServer, email);
    }

    public void registerDatabaseConnector(DatabaseConnectorRegistration<?, ?> registration, Class<?> clazz) {
        databaseConnectors.put(clazz, registration);
    }

    @Override
    public PlatformHandle<P, S> getPlatformHandle() {
        return platformHandle;
    }

    protected abstract PlatformHandle<P, S> providePlatformHandle();

    @Override
    public SemanticVersion getParsedVersion() {
        return version;
    }


    @Override
    public boolean validPassword(String password) {
        return validatePassword(password) == null;
    }
    
    /**
     * Validates a password and returns the specific error message key if invalid, null if valid.
     * Note: Length validation should be done separately with detailed error message.
     * @param password The password to validate
     * @return The message key for the specific validation error, or null if password is valid
     */
    public String validatePassword(String password) {
        // Check for uppercase requirement
        if (configuration.get(PASSWORD_REQUIRE_UPPERCASE)) {
            if (!password.chars().anyMatch(Character::isUpperCase)) {
                return "error-password-no-uppercase";
            }
        }

        // Check for number requirement
        if (configuration.get(PASSWORD_REQUIRE_NUMBER)) {
            if (!password.chars().anyMatch(Character::isDigit)) {
                return "error-password-no-number";
            }
        }

        // Check for special character requirement
        if (configuration.get(PASSWORD_REQUIRE_SPECIAL_CHAR)) {
            String specialChars = "!@#$%^&*()-_+={}|\\:;'\"<>,.?/~`[]";
            if (!password.chars().anyMatch(c -> specialChars.indexOf(c) >= 0)) {
                return "error-password-no-special-char";
            }
        }

        return null;
    }
    
    /**
     * Checks if a password is weak (doesn't meet the optional strength requirements)
     * @param password The password to check
     * @return true if password is weak (doesn't meet optional requirements), false otherwise
     */
    public boolean isWeakPassword(String password) {
        // Password is weak if any of the optional requirements are not met
        boolean hasUppercase = password.chars().anyMatch(Character::isUpperCase);
        boolean hasNumber = password.chars().anyMatch(Character::isDigit);
        String specialChars = "!@#$%^&*()-_+={}|\\:;'\"<>,.?/~`[]";
        boolean hasSpecialChar = password.chars().anyMatch(c -> specialChars.indexOf(c) >= 0);
        
        var minLength = configuration.get(MINIMUM_PASSWORD_LENGTH);
        boolean meetsMinLength = minLength <= 0 || password.length() >= minLength;
        
        // If any recommended requirement is not met, password is weak
        return !hasUppercase || !hasNumber || !hasSpecialChar || !meetsMinLength;
    }

    @Override
    public Map<String, ReadDatabaseProviderRegistration<?, ?, ?>> getReadProviders() {
        return Map.copyOf(readProviders);
    }

    public CommandProvider<P, S> getCommandProvider() {
        return commandProvider;
    }

    @Override
    public ReadWriteDatabaseProvider getDatabaseProvider() {
        return databaseProvider;
    }

    @Override
    public AuthenticPremiumProvider getPremiumProvider() {
        return premiumProvider;
    }

    @Override
    public AuthenticImageProjector<P, S> getImageProjector() {
        return imageProjector;
    }

    @Override
    public ServerHandler<P, S> getServerHandler() {
        return serverHandler;
    }

    protected void enable() {
        version = SemanticVersion.parse(getVersion());
        if (logger == null) logger = provideLogger();

        try {
            new Log4JFilter().inject();
        } catch (Throwable ignored) {
            logger.info("LogFilter is not supported on this platform");
            var simpleLogger = getSimpleLogger();

            if (simpleLogger != null) {
                logger.info("Using SimpleLogFilter");
                new SimpleLogFilter(simpleLogger).inject();
            }
        }

        var folder = getDataFolder();
        if (!folder.exists()) {
            var oldFolder = new File(folder.getParentFile(), folder.getName().equals("librelogin") ? "librepremium" : "LibrePremium");
            if (oldFolder.exists()) {
                logger.info("Migrating configuration and messages from old folder...");
                if (!oldFolder.renameTo(folder)) {
                    throw new RuntimeException("Can't migrate configuration and messages from old folder!");
                }
            }
        }

        try {
            Files.copy(getResourceAsStream("LICENSE.txt"), new File(folder, "LICENSE.txt").toPath(), StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException ignored) {
            // Silently ignore
        }

        if (platformHandle.getPlatformIdentifier().equals("paper")) {
            LIMBO.setDefault(List.of("limbo"));

            var lobby = HashMultimap.<String, String>create();
            lobby.put("root", "world");

            LOBBY.setDefault(lobby);
        }

        eventProvider = new AuthenticEventProvider<>(this);
        premiumProvider = new AuthenticPremiumProvider(this);

        registerCryptoProvider(new MessageDigestCryptoProvider("SHA-256"));
        registerCryptoProvider(new MessageDigestCryptoProvider("SHA-512"));
        registerCryptoProvider(new BCrypt2ACryptoProvider());
        registerCryptoProvider(new Argon2IDCryptoProvider(logger));
        registerCryptoProvider(new LogITMessageDigestCryptoProvider("LOGIT-SHA-256", "SHA-256"));

        setupDB();

        checkDataFolder();

        loadConfigs();
        connectToDB();

        serverHandler = new AuthenticServerHandler<>(this);

        this.loginTryListener = new LoginTryListener<>(this);

        // Data migration feature removed - no longer checking and migrating
        // GeneralUtil.checkAndMigrate(configuration, logger, this);

        // TOTP/2FA feature removed - image projector no longer needed
        // imageProjector = provideImageProjector();
        // if (imageProjector != null) {
        //     if (!configuration.get(TOTP_ENABLED)) {
        //         imageProjector = null;
        //         logger.warn("2FA is disabled in the configuration, aborting...");
        //     } else {
        //         imageProjector.enable();
        //     }
        // }

        eMailHandler = configuration.get(MAIL_ENABLED) ? new AuthenticEMailHandler(this) : null;

        authorizationProvider = new AuthenticAuthorizationProvider<>(this);
        commandProvider = new CommandProvider<>(this);

        if (version.dev()) {
            logger.warn("!! YOU ARE RUNNING A DEVELOPMENT BUILD OF LIBRELOGIN !!");
            logger.warn("!! THIS IS NOT A RELEASE, USE THIS ONLY IF YOU WERE INSTRUCTED TO DO SO. DO NOT USE THIS IN PRODUCTION !!");
        } else {
            initMetrics();
        }

        delay(this::checkForUpdates, 1000);

        if (pluginPresent("floodgate")) {
            logger.info("Floodgate detected, enabling bedrock support...");
            floodgateApi = new FloodgateIntegration();
        }

        if (pluginPresent("luckperms")) {
            logger.info("LuckPerms detected, enabling context provider");
            luckpermsApi = new LuckPermsIntegration<>(this);
        }

        if (multiProxyEnabled()) {
            logger.info("Detected MultiProxy setup, enabling MultiProxy support...");
        }
    }

    public <C extends DatabaseConnector<?, ?>> DatabaseConnectorRegistration<?, C> getDatabaseConnector(Class<C> clazz) {
        return (DatabaseConnectorRegistration<?, C>) databaseConnectors.get(clazz);
    }

    private void connectToDB() {
        logger.info("Connecting to the database...");

        try {
            var registration = readProviders.get(configuration.get(DATABASE_TYPE));
            if (registration == null) {
                logger.error("Database type %s doesn't exist, please check your configuration".formatted(configuration.get(DATABASE_TYPE)));
                shutdownProxy(1);
            }

            DatabaseConnector<?, ?> connector = null;

            if (registration.databaseConnector() != null) {
                var connectorRegistration = getDatabaseConnector(registration.databaseConnector());

                if (connectorRegistration == null) {
                    logger.error("Database type %s is corrupted, please use a different one".formatted(configuration.get(DATABASE_TYPE)));
                    shutdownProxy(1);
                }

                connector = connectorRegistration.factory().apply("database.properties." + connectorRegistration.id() + ".");

                connector.connect();
            }

            var provider = registration.create(connector);

            if (provider instanceof ReadWriteDatabaseProvider casted) {
                databaseProvider = casted;
                databaseConnector = connector;
            } else {
                logger.error("Database type %s cannot be used for writing, please use a different one".formatted(configuration.get(DATABASE_TYPE)));
                shutdownProxy(1);
            }

        } catch (Exception e) {
            var cause = GeneralUtil.getFurthestCause(e);
            logger.error("!! THIS IS MOST LIKELY NOT AN ERROR CAUSED BY LIBRELOGIN !!");
            logger.error("Failed to connect to the database, this most likely is caused by wrong credentials. Cause: %s: %s".formatted(cause.getClass().getSimpleName(), cause.getMessage()));
            shutdownProxy(1);
        }

        logger.info("Successfully connected to the database");

        if (databaseProvider instanceof AuthenticDatabaseProvider<?> casted) {
            logger.info("Validating schema");

            try {
                casted.validateSchema();
            } catch (Exception e) {
                var cause = GeneralUtil.getFurthestCause(e);
                logger.error("Failed to validate schema! Cause: %s: %s".formatted(cause.getClass().getSimpleName(), cause.getMessage()));
                logger.error("Please open an issue on our GitHub, or visit Discord support");
                shutdownProxy(1);
            }

            logger.info("Schema validated");
        }
    }

    private void loadConfigs() {
        logger.info("Loading messages...");

        messages = new HoconMessages(logger);

        try {
            messages.reload(this);
        } catch (IOException e) {
            e.printStackTrace();
            logger.info("An unknown exception occurred while attempting to load the messages, this most likely isn't your fault");
            shutdownProxy(1);
        } catch (CorruptedConfigurationException e) {
            var cause = GeneralUtil.getFurthestCause(e);
            logger.error("!! THIS IS MOST LIKELY NOT AN ERROR CAUSED BY LIBRELOGIN !!");
            logger.error("!!The messages are corrupted, please look below for further clues. If you are clueless, delete the messages and a new ones will be created for you. Cause: %s: %s".formatted(cause.getClass().getSimpleName(), cause.getMessage()));
            shutdownProxy(1);
        }

        logger.info("Loading configuration...");

        var defaults = new ArrayList<BiHolder<Class<?>, String>>();

        for (DatabaseConnectorRegistration<?, ?> value : databaseConnectors.values()) {
            if (value.configClass() == null) continue;
            defaults.add(new BiHolder<>(value.configClass(), "database.properties." + value.id() + "."));
            // Migration config removed
            // defaults.add(new BiHolder<>(value.configClass(), "migration.old-database." + value.id() + "."));
        }

        configuration = new HoconPluginConfiguration(logger, defaults);

        try {
            if (configuration.reload(this)) {
                logger.warn("!! A new configuration was generated, please fill it out, if in doubt, see the wiki !!");
                shutdownProxy(0);
            }

            var limbos = configuration.get(LIMBO);
            var lobby = configuration.get(LOBBY);

            for (String value : lobby.values()) {
                if (limbos.contains(value)) {
                    throw new CorruptedConfigurationException("Lobby server/world %s is also a limbo server/world, this is not allowed".formatted(value));
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
            logger.info("An unknown exception occurred while attempting to load the configuration, this most likely isn't your fault");
            shutdownProxy(1);
        } catch (CorruptedConfigurationException e) {
            var cause = GeneralUtil.getFurthestCause(e);
            logger.error("!! THIS IS MOST LIKELY NOT AN ERROR CAUSED BY LIBRELOGIN !!");
            logger.error("!!The configuration is corrupted, please look below for further clues. If you are clueless, delete the config and a new one will be created for you. Cause: %s: %s".formatted(cause.getClass().getSimpleName(), cause.getMessage()));
            shutdownProxy(1);
        }
    }

    private void setupDB() {
        registerDatabaseConnector(new DatabaseConnectorRegistration<>(
                        prefix -> new AuthenticMySQLDatabaseConnector(this, prefix),
                        AuthenticMySQLDatabaseConnector.Configuration.class,
                        "mysql"
                ),
                MySQLDatabaseConnector.class);
        registerDatabaseConnector(new DatabaseConnectorRegistration<>(
                        prefix -> new AuthenticSQLiteDatabaseConnector(this, prefix),
                        AuthenticSQLiteDatabaseConnector.Configuration.class,
                        "sqlite"
                ),
                SQLiteDatabaseConnector.class);
        registerDatabaseConnector(new DatabaseConnectorRegistration<>(
                        prefix -> new AuthenticPostgreSQLDatabaseConnector(this, prefix),
                        AuthenticPostgreSQLDatabaseConnector.Configuration.class,
                        "postgresql"
                ),
                PostgreSQLDatabaseConnector.class);

        registerReadProvider(new ReadDatabaseProviderRegistration<>(
                connector -> new LibreLoginMySQLDatabaseProvider(connector, this),
                "librelogin-mysql",
                MySQLDatabaseConnector.class
        ));
        registerReadProvider(new ReadDatabaseProviderRegistration<>(
                connector -> new LibreLoginSQLiteDatabaseProvider(connector, this),
                "librelogin-sqlite",
                SQLiteDatabaseConnector.class
        ));
        registerReadProvider(new ReadDatabaseProviderRegistration<>(
                connector -> new LibreLoginPostgreSQLDatabaseProvider(connector, this),
                "librelogin-postgresql",
                PostgreSQLDatabaseConnector.class
        ));
        
        // Migration providers removed - data migration feature removed
        // All *MigrateReadProvider registrations have been removed
    }

    private void checkForUpdates() {
        logger.info("Checking for updates...");

        try {
            var connection = new URL("https://api.github.com/repos/kyngs/LibreLogin/releases").openConnection();

            connection.setRequestProperty("User-Agent", "LibreLogin");

            var in = connection.getInputStream();

            var root = GSON.fromJson(new InputStreamReader(in), JsonArray.class);

            in.close(); //Not the safest way, but a slight leak isn't a big deal

            List<Release> behind = new ArrayList<>();
            SemanticVersion latest = null;

            for (JsonElement raw : root) {
                var release = raw.getAsJsonObject();

                var version = SemanticVersion.parse(release.get("tag_name").getAsString());

                if (latest == null) latest = version;

                var shouldBreak = switch (this.version.compare(version)) {
                    case 0, 1 -> true;
                    default -> {
                        behind.add(new Release(version, release.get("name").getAsString()));
                        yield false;
                    }
                };

                if (shouldBreak) {
                    break;
                }
            }

            if (behind.isEmpty()) {
                logger.info("You are running the latest version of LibreLogin");
            } else {
                Collections.reverse(behind);
                logger.warn("!! YOU ARE RUNNING AN OUTDATED VERSION OF LIBRELOGIN !!");
                logger.info("You are running version %s, the latest version is %s. You are running %s versions behind. Newer versions:".formatted(getVersion(), latest, behind.size()));
                for (Release release : behind) {
                    logger.info("- %s".formatted(release.name()));
                }
                logger.warn("!! PLEASE UPDATE TO THE LATEST VERSION !!");
            }
        } catch (Exception e) {
            logger.warn("Failed to check for updates", e);
        }
    }

    public UUID generateNewUUID(String name, @Nullable UUID premiumID) {
        return switch (configuration.getNewUUIDCreator()) {
            case RANDOM -> UUID.randomUUID();
            case MOJANG -> premiumID == null ? GeneralUtil.getCrackedUUIDFromName(name) : premiumID;
            case CRACKED -> GeneralUtil.getCrackedUUIDFromName(name);
        };
    }

    protected void disable() {
        if (databaseConnector != null) {
            try {
                databaseConnector.disconnect();
            } catch (Exception e) {
                e.printStackTrace();
                logger.error("Failed to disconnect from database, ignoring...");
            }
        }
        if (luckpermsApi != null) {
            luckpermsApi.disable();
        }
    }

    @Override
    public Logger getLogger() {
        return logger;
    }

    public HoconPluginConfiguration getConfiguration() {
        return configuration;
    }

    @Override
    public HoconMessages getMessages() {
        return messages;
    }

    @Override
    public void checkDataFolder() {
        var folder = getDataFolder();

        if (!folder.exists()) if (!folder.mkdir()) throw new RuntimeException("Failed to create datafolder");
    }

    protected abstract Logger provideLogger();

    public abstract CommandManager<?, ?, ?, ?, ?, ?> provideManager();

    public abstract P getPlayerFromIssuer(CommandIssuer issuer);

    public abstract void authorize(P player, User user, Audience audience);

    public abstract CancellableTask delay(Runnable runnable, long delayInMillis);

    public abstract CancellableTask repeat(Runnable runnable, long delayInMillis, long repeatInMillis);

    public abstract boolean pluginPresent(String pluginName);

    protected abstract AuthenticImageProjector<P, S> provideImageProjector();

    public PremiumUser getUserOrThrowICA(String username) throws InvalidCommandArgument {
        try {
            return getPremiumProvider().getUserForName(username);
        } catch (PremiumException e) {
            throw new InvalidCommandArgument(getMessages().getMessage(
                    switch (e.getIssue()) {
                        case THROTTLED -> "error-premium-throttled";
                        default -> "error-premium-unknown";
                    }
            ));
        }
    }

    protected abstract void initMetrics(CustomChart... charts);

    @Override
    public AuthenticAuthorizationProvider<P, S> getAuthorizationProvider() {
        return authorizationProvider;
    }

    @Override
    public CryptoProvider getCryptoProvider(String id) {
        return cryptoProviders.get(id);
    }

    @Override
    public void registerCryptoProvider(CryptoProvider provider) {
        cryptoProviders.put(provider.getIdentifier(), provider);
    }

    @Override
    public CryptoProvider getDefaultCryptoProvider() {
        return getCryptoProvider(configuration.get(DEFAULT_CRYPTO_PROVIDER));
    }

    @Override
    public void migrate(ReadDatabaseProvider from, WriteDatabaseProvider to) {
        logger.info("Reading data...");
        var users = from.getAllUsers();
        logger.info("Data read, inserting into database...");
        to.insertUsers(users);
    }

    @Override
    public AuthenticEventProvider<P, S> getEventProvider() {
        return eventProvider;
    }

    public LoginTryListener<P, S> getLoginTryListener() {
        return loginTryListener;
    }

    public void onExit(P player) {
        cancelOnExit.removeAll(player).forEach(CancellableTask::cancel);
        if (configuration.get(REMEMBER_LAST_SERVER)) {
            var server = platformHandle.getPlayersServerName(player);
            if (server == null) return;
            var user = databaseProvider.getByUUID(platformHandle.getUUIDForPlayer(player));
            if (user != null && !getConfiguration().get(LIMBO).contains(server)) {
                user.setLastServer(server);
                databaseProvider.updateUser(user);
            }
        }
    }

    public void cancelOnExit(CancellableTask task, P player) {
        cancelOnExit.put(player, task);
    }

    public boolean floodgateEnabled() {
        return floodgateApi != null;
    }

    public boolean luckpermsEnabled() {
        return luckpermsApi != null;
    }

    public boolean fromFloodgate(UUID uuid) {
        return floodgateApi != null && uuid != null && floodgateApi.isFloodgateId(uuid);
    }

    protected void shutdownProxy(int code) {
        //noinspection finally
        try {
            Thread.sleep(5000);
        } catch (InterruptedException ignored) {
        } finally {
            System.exit(code);
        }
    }

    public abstract Audience getAudienceFromIssuer(CommandIssuer issuer);

    protected boolean mainThread() {
        return false;
    }

    public void reportMainThread() {
        if (mainThread()) {
            logger.error("AN IO OPERATION IS BEING PERFORMED ON THE MAIN THREAD! THIS IS A SERIOUS BUG!, PLEASE REPORT IT TO THE DEVELOPER OF THE PLUGIN AND ATTACH THE STACKTRACE BELOW!");
            new Throwable().printStackTrace();
        }
    }

    public boolean fromFloodgate(String username) {
        return floodgateApi != null && floodgateApi.getPlayer(username) != null;
    }

    protected java.util.logging.Logger getSimpleLogger() {
        return null;
    }
}