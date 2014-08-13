package de.probst.ba.core.media;

import java.util.List;
import java.util.concurrent.ConcurrentMap;

/**
 * Created by chrisprobst on 13.08.14.
 */
public interface DataBase<D> {

    /**
     * @return All registered data info in this data base.
     */
    ConcurrentMap<String, DataInfo> getDataInfo();

    /**
     * Get all chunks according to the data info.
     *
     * @param dataInfo
     * @return
     */
    List<D> getChunks(DataInfo dataInfo);
}
