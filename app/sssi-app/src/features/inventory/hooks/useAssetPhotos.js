import { useState, useEffect } from 'react';

export function useAssetPhotos(open) {
    const [photos, setPhotos] = useState([]);
    const [existingPhotos, setExistingPhotos] = useState([]);
    const [photosToDelete, setPhotosToDelete] = useState([]);
    const [isDragOver, setIsDragOver] = useState(false);

    useEffect(() => {
        if (!open) return undefined;

        const resetHandle = window.setTimeout(() => {
            setPhotos([]);
            setExistingPhotos([]);
            setPhotosToDelete([]);
            setIsDragOver(false);
        }, 0);

        return () => window.clearTimeout(resetHandle);
    }, [open]);

    return {
        photos,
        setPhotos,
        existingPhotos,
        setExistingPhotos,
        photosToDelete,
        setPhotosToDelete,
        isDragOver,
        setIsDragOver,
    };
}
