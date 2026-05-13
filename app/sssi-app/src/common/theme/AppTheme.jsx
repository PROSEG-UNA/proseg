import * as React from 'react';
import { ThemeProvider, createTheme } from '@mui/material/styles';
import { inputsCustomizations } from './customizations/inputs';
import { dataDisplayCustomizations } from './customizations/dataDisplay';
import { feedbackCustomizations } from './customizations/feedback';
import { navigationCustomizations } from './customizations/navigation';
import { surfacesCustomizations } from './customizations/surfaces';
import { dataGridCustomizations } from './customizations/dataGrid';
import { datePickersCustomizations } from './customizations/datePickers';
import { colorSchemes, typography, shadows, shape } from './themePrimitives';

export default function AppTheme({ children, disableCustomTheme, themeComponents, defaultColorScheme = 'light' }) {
    const theme = React.useMemo(() => {
        return disableCustomTheme
            ? {}
            : createTheme({
                cssVariables: {
                    colorSchemeSelector: 'data-mui-color-scheme',
                    cssVarPrefix: 'template',
                },
                colorSchemes,
                typography,
                shadows,
                shape,
                components: {
                    ...inputsCustomizations,
                    ...dataDisplayCustomizations,
                    ...feedbackCustomizations,
                    ...navigationCustomizations,
                    ...surfacesCustomizations,
                    ...dataGridCustomizations,
                    ...datePickersCustomizations,
                    ...themeComponents,
                },
            });
    }, [disableCustomTheme, themeComponents]);
    if (disableCustomTheme) {
        return <React.Fragment>{children}</React.Fragment>;
    }
    return (
        <ThemeProvider theme={theme} defaultMode={defaultColorScheme} disableTransitionOnChange>
            {children}
        </ThemeProvider>
    );
}
