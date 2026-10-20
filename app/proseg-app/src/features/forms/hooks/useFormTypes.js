import { useQuery } from '@tanstack/react-query';
import { queryKeys } from '../../../common/query';
import { fetchFormTypes } from '../services/formsService';
import { getFriendlyApiErrorMessage } from '../../../common/utils';

export function useFormTypes() {
    const { data, isLoading, isFetching, error } = useQuery({
        queryKey: queryKeys.forms.types(),
        queryFn: fetchFormTypes,
    });

    return {
        formTypes: data ?? [],
        loading: isLoading,
        fetching: isFetching,
        error: error ? getFriendlyApiErrorMessage(error, 'Error al cargar tipos de formulario') : null,
    };
}
