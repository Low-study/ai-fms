import { useState, useCallback } from 'react';
import { Input, Select } from 'antd';
import { SearchOutlined } from '@ant-design/icons';
import { useTranslation } from 'react-i18next';

export interface SearchFilter {
  label: string;
  value: string;
}

export interface SearchFormProps {
  placeholder?: string;
  onSearch: (keyword: string, filters: string[]) => void;
  filters?: SearchFilter[];
  loading?: boolean;
}

export default function SearchForm({ placeholder, onSearch, filters, loading = false }: SearchFormProps) {
  const { t } = useTranslation();
  const [keyword, setKeyword] = useState('');
  const [selectedFilters, setSelectedFilters] = useState<string[]>([]);

  const handleSearch = useCallback(
    (value: string) => {
      onSearch(value.trim(), selectedFilters);
    },
    [onSearch, selectedFilters],
  );

  const handleFilterChange = useCallback(
    (values: string[]) => {
      setSelectedFilters(values);
      onSearch(keyword.trim(), values);
    },
    [onSearch, keyword],
  );

  const handleKeywordChange = useCallback(
    (e: React.ChangeEvent<HTMLInputElement>) => {
      setKeyword(e.target.value);
    },
    [],
  );

  return (
    <div className="search-bar">
      <Input.Search
        placeholder={placeholder ?? t('common.search')}
        value={keyword}
        onChange={handleKeywordChange}
        onSearch={handleSearch}
        onPressEnter={() => handleSearch(keyword)}
        style={{ maxWidth: 400, flex: '1 1 auto' }}
        allowClear
        loading={loading}
        size="middle"
        prefix={<SearchOutlined />}
      />
      {filters && filters.length > 0 && (
        <Select
          mode="multiple"
          placeholder={t('common.search')}
          value={selectedFilters}
          onChange={handleFilterChange}
          style={{ minWidth: 180 }}
          size="middle"
          allowClear
          maxTagCount={2}
          options={filters.map((f) => ({
            label: f.label,
            value: f.value,
          }))}
        />
      )}
    </div>
  );
}
