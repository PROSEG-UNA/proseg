export const surfaceSx = (t) => ({
  background: 'hsla(220, 30%, 100%, 0.85)',
  ...t.applyStyles('dark', {
    background: `
      radial-gradient(ellipse 70% 120% at 50% -10%, rgba(196, 30, 58, 0.06) 0%, transparent 65%),
      linear-gradient(180deg, rgba(30, 12, 15, 0.95) 0%, rgba(20, 10, 12, 0.92) 100%)
    `,
    boxShadow: '0 1px 0 rgba(255,255,255,0.03)',
    borderColor: 'rgba(255,255,255,0.06)',
  }),
});
