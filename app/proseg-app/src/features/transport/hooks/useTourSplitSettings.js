import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { fetchTourSplitSettings, updateTourSplitSettings } from '../services/tourSplit/tourSplitService';
import { queryKeys } from '../../../common/query';

export function useTourSplitSettings() {
    const queryClient = useQueryClient();
    const { data, isLoading } = useQuery({
        queryKey: queryKeys.transport.tourSplitSettings(),
        queryFn: fetchTourSplitSettings,
    });

    const mutation = useMutation({
        mutationFn: (defaultMode) => updateTourSplitSettings({ defaultMode }),
        onSuccess: (saved) => {
            queryClient.setQueryData(queryKeys.transport.tourSplitSettings(), saved);
        },
        onSettled: () => {
            void queryClient.invalidateQueries({ queryKey: queryKeys.transport.tourSplitSettings() });
        },
    });

    return {
        defaultMode: data?.defaultMode ?? null,
        loading: isLoading,
        saving: mutation.isPending,
        error: mutation.error,
        changeDefaultMode: mutation.mutate,
    };
}
