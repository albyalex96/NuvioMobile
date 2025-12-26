export type TVFocusPresetName = 'card' | 'poster' | 'pill' | 'button' | 'icon' | 'listRow';

export type TVFocusPreset = {
  focusScale: number;
  focusRingWidth: number;
  /**
   * Border radius should be passed by the caller when it is dynamic.
   * This is a sensible default.
   */
  focusBorderRadius: number;
};

export const tvFocusPresets: Record<TVFocusPresetName, TVFocusPreset> = {
  card: { focusScale: 1.05, focusRingWidth: 3, focusBorderRadius: 16 },
  poster: { focusScale: 1.06, focusRingWidth: 3, focusBorderRadius: 12 },
  pill: { focusScale: 1.04, focusRingWidth: 3, focusBorderRadius: 999 },
  button: { focusScale: 1.04, focusRingWidth: 3, focusBorderRadius: 18 },
  icon: { focusScale: 1.06, focusRingWidth: 3, focusBorderRadius: 999 },
  listRow: { focusScale: 1.03, focusRingWidth: 3, focusBorderRadius: 14 },
};

