export const headerSurfaceSx = (t, { gradient = false } = {}) => ({
  background: 'hsl(220, 20%, 99%)',
  boxShadow: '0 2px 8px rgba(100, 108, 130, 0.10)',
  ...t.applyStyles('dark', {
    background: gradient
      ? `linear-gradient(180deg, rgb(18, 17, 18) 0%, rgb(13, 11, 13) 100%)`
      : 'rgb(18, 17, 18)',
    boxShadow: '0 2px 8px rgba(0, 0, 0, 0.4)',
  }),
});

export const panelSurfaceSx = (t, { gradient = false } = {}) => ({
  background: gradient
    ? `
        radial-gradient(ellipse 80% 40% at 50% 0%, hsla(0, 70%, 55%, 0.05) 0%, transparent 70%),
        linear-gradient(180deg, hsl(220, 20%, 99%) 0%, hsl(220, 18%, 97%) 100%)
      `
    : 'hsl(220, 20%, 99%)',
  borderColor: 'divider',
  ...t.applyStyles('dark', {
    background: gradient
      ? `linear-gradient(180deg, rgb(23, 21, 22) 0%, rgb(19, 16, 17) 100%)`
      : 'rgb(23, 21, 22)',
    borderColor: 'rgba(255, 255, 255, 0.06)',
  }),
});
