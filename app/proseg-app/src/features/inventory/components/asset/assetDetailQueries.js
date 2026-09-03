import { fetchNetworkInterfaceByAsset } from '../../services/assetsService';
import { fetchAssetArchives } from '../../services/assetArchiveService';
import { fetchAssetComponents } from '../../services/assetComponentsService';
import { queryKeys } from '../../../../common/query';

export function assetDetailQueryOptions(assetId) {
    return {
        queryKey: queryKeys.inventory.assetDetail(assetId),
        queryFn: async () => {
            const [iface, archives] = await Promise.all([
                fetchNetworkInterfaceByAsset(assetId),
                fetchAssetArchives(assetId),
            ]);
            return { netIface: iface, images: archives.filter(archive => !!archive.imageUrl) };
        },
    };
}

export function assetComponentsQueryOptions(assetId) {
    return {
        queryKey: queryKeys.inventory.assetComponents(assetId),
        queryFn: () => fetchAssetComponents(assetId),
    };
}
