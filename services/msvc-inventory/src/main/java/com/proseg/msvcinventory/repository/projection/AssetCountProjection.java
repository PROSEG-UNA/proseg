package com.proseg.msvcinventory.repository.projection;

import java.util.UUID;

public interface AssetCountProjection {

    UUID getAssetId();

    long getTotal();
}
