import React, { memo, useCallback, useRef } from 'react';
import { View, Text, StyleSheet, FlatList, Platform } from 'react-native';
import { FocusableTouchableOpacity } from './common/FocusableTouchableOpacity';

interface ProviderFilterProps {
  selectedProvider: string;
  providers: Array<{ id: string; name: string; }>;
  onSelect: (id: string) => void;
  theme: any;
}

const ProviderFilter = memo(({ 
  selectedProvider, 
  providers, 
  onSelect,
  theme
}: ProviderFilterProps) => {
  const styles = React.useMemo(() => createStyles(theme.colors), [theme.colors]);
  const listRef = useRef<FlatList<any> | null>(null);
  
  const renderItem = useCallback(({ item, index }: { item: { id: string; name: string }; index: number }) => (
    <FocusableTouchableOpacity
      style={[
        styles.filterChip,
        selectedProvider === item.id && styles.filterChipSelected
      ]}
      onPress={() => onSelect(item.id)}
      enableTVFocus={Platform.isTV}
      preset="pill"
      focusBorderRadius={16}
      onFocus={() => {
        if (!Platform.isTV) return;
        try {
          listRef.current?.scrollToIndex({ index, animated: true, viewPosition: 0.5 });
        } catch { }
      }}
    >
      <Text style={[
        styles.filterChipText,
        selectedProvider === item.id && styles.filterChipTextSelected
      ]}>
        {item.name}
      </Text>
    </FocusableTouchableOpacity>
  ), [selectedProvider, onSelect, styles]);

  return (
    <View>
      <FlatList
        ref={listRef}
        data={providers}
        renderItem={renderItem}
        keyExtractor={item => item.id}
        horizontal
        showsHorizontalScrollIndicator={false}
        style={styles.filterScroll}
        bounces={true}
        overScrollMode="never"
        decelerationRate="fast"
        initialNumToRender={5}
        maxToRenderPerBatch={3}
        windowSize={3}
        removeClippedSubviews={true}
        getItemLayout={(data, index) => ({
          length: 100, // Approximate width of each item
          offset: 100 * index,
          index,
        })}
      />
    </View>
  );
});

const createStyles = (colors: any) => StyleSheet.create({
  filterScroll: {
    flexGrow: 0,
  },
  filterChip: {
    backgroundColor: colors.elevation2,
    paddingHorizontal: 14,
    paddingVertical: 8,
    borderRadius: 16,
    marginRight: 8,
    borderWidth: 0,
  },
  filterChipSelected: {
    backgroundColor: colors.primary,
  },
  filterChipText: {
    color: colors.highEmphasis,
    fontWeight: '600',
    letterSpacing: 0.1,
  },
  filterChipTextSelected: {
    color: colors.white,
    fontWeight: '700',
  },
});

export default ProviderFilter;
