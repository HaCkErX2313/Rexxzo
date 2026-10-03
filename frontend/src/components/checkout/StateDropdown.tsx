'use client';

import React, { useState, useRef, useEffect } from 'react';
import { ChevronDown, Search, Check, X } from 'lucide-react';
import { INDIAN_STATES } from '@/constants/indianStates';

interface StateDropdownProps {
  value: string;
  onChange: (state: string) => void;
  disabled?: boolean;
  hasError?: boolean;
}

export default function StateDropdown({
  value,
  onChange,
  disabled = false,
  hasError = false,
}: StateDropdownProps) {
  const [isOpen, setIsOpen] = useState(false);
  const [search, setSearch] = useState('');
  const containerRef = useRef<HTMLDivElement>(null);
  const inputRef = useRef<HTMLInputElement>(null);

  const filteredStates = INDIAN_STATES.filter((s) =>
    s.toLowerCase().includes(search.toLowerCase())
  );

  useEffect(() => {
    const handleClickOutside = (e: MouseEvent) => {
      if (containerRef.current && !containerRef.current.contains(e.target as Node)) {
        setIsOpen(false);
      }
    };
    document.addEventListener('mousedown', handleClickOutside);
    return () => document.removeEventListener('mousedown', handleClickOutside);
  }, []);

  const handleSelect = (state: string) => {
    onChange(state);
    setIsOpen(false);
    setSearch('');
  };

  return (
    <div ref={containerRef} className="relative w-full">
      <button
        type="button"
        disabled={disabled}
        onClick={() => {
          if (!disabled) {
            setIsOpen((prev) => !prev);
            setTimeout(() => inputRef.current?.focus(), 50);
          }
        }}
        className={`w-full bg-[#f9f8f6] border rounded p-2.5 text-xs flex items-center justify-between text-left transition-colors cursor-pointer disabled:opacity-50 disabled:cursor-not-allowed ${
          hasError
            ? 'border-red-400 focus:border-red-500'
            : 'border-[#e5e1d8] focus:border-[#C8BCA7]'
        } ${value ? 'text-[#080808] font-medium' : 'text-[#888888]'}`}
      >
        <span className="truncate">{value || 'Select Indian State / UT'}</span>
        <ChevronDown className={`w-3.5 h-3.5 text-[#888888] transition-transform flex-shrink-0 ml-1 ${isOpen ? 'rotate-180' : ''}`} />
      </button>

      {isOpen && (
        <div className="absolute z-50 mt-1 w-full bg-white border border-[#e5e1d8] rounded-lg shadow-xl overflow-hidden animate-in fade-in zoom-in-95 duration-100 max-h-72 flex flex-col">
          {/* Search bar inside dropdown */}
          <div className="p-2 border-b border-[#f0ece5] bg-[#fcfbfa] flex items-center gap-2">
            <Search className="w-3.5 h-3.5 text-[#888888] flex-shrink-0" />
            <input
              ref={inputRef}
              type="text"
              value={search}
              onChange={(e) => setSearch(e.target.value)}
              placeholder="Search State or UT..."
              className="w-full bg-transparent text-xs text-[#080808] outline-none placeholder:text-[#999999]"
            />
            {search && (
              <button
                type="button"
                onClick={() => setSearch('')}
                className="text-[#888888] hover:text-[#080808] p-0.5"
              >
                <X className="w-3 h-3" />
              </button>
            )}
          </div>

          {/* List of States */}
          <div className="overflow-y-auto max-h-56 divide-y divide-[#f5f2eb]">
            {filteredStates.length > 0 ? (
              filteredStates.map((state) => {
                const isSelected = value.toLowerCase() === state.toLowerCase();
                return (
                  <button
                    key={state}
                    type="button"
                    onClick={() => handleSelect(state)}
                    className={`w-full px-3 py-2 text-xs flex items-center justify-between text-left hover:bg-[#f6f3ed] transition-colors cursor-pointer ${
                      isSelected ? 'bg-[#f0ece5] font-bold text-[#080808]' : 'text-[#333333]'
                    }`}
                  >
                    <span>{state}</span>
                    {isSelected && <Check className="w-3.5 h-3.5 text-[#080808] flex-shrink-0" />}
                  </button>
                );
              })
            ) : (
              <div className="px-3 py-4 text-center text-xs text-[#888888]">
                No matching state or UT found
              </div>
            )}
          </div>
        </div>
      )}
    </div>
  );
}
