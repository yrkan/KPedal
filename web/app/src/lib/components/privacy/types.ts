export interface DataRow {
  field: string;
  purpose: string;
}

export interface SummaryItem {
  icon: 'shield' | 'no-location' | 'no-money' | 'delete';
  titleKey: string;
  descKey: string;
}

export interface FlowStep {
  number: number;
  titleKey: string;
  descKey: string;
}

export interface SecurityItem {
  titleKey: string;
  descKey: string;
}

export interface RightCard {
  titleKey: string;
  descKey: string;
}

export interface ThirdParty {
  titleKey: string;
  descKey: string;
  linkText: string;
  linkUrl: string;
}

export interface TocItem {
  id: string;
  labelKey: string;
}
