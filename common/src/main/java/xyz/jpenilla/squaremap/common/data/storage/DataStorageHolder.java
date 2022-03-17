package xyz.jpenilla.squaremap.common.data.storage;

import xyz.jpenilla.squaremap.common.config.Config;

public class DataStorageHolder {

    private static DataStorage dataStorage;

    public static DataStorage getDataStorage() {
        if (dataStorage == null) {
            switch (Config.STORAGE_TYPE) {
                case FLATFILE -> dataStorage = new FlatfileDataStorage();
            }
        }
        return dataStorage;
    }

}
