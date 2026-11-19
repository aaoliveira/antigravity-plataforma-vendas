export interface CompanyDto {
  id: string;
  tenantId: string;
  corporateName: string;
  tradeName: string | null;
  cnpj: string;
  formattedCnpj: string;
  stateRegistration: string | null;
  taxRegime: string | null;
  isActive: boolean;
  notes: string | null;
  createdAt: string;
  updatedAt: string;
}

export interface CompanyAccountLinkDto {
  id: string;
  tenantId: string;
  companyId: string;
  companyName: string;
  sellerAccountId: string;
  marketplace: string;
  accountName: string;
  effectiveFrom: string;
  effectiveTo: string | null;
  isActive: boolean;
  version: number;
  notes: string | null;
  linkedBy: string;
  createdAt: string;
  updatedAt: string;
}

export interface SellerAccountDto {
  id: string;
  marketplace: string;
  externalAccountId: string;
  accountName: string;
  status: string;
}

export interface CreateCompanyInput {
  corporateName: string;
  tradeName?: string;
  cnpj: string;
  stateRegistration?: string;
  taxRegime?: string;
  notes?: string;
}

export interface UpdateCompanyInput {
  corporateName: string;
  tradeName?: string;
  stateRegistration?: string;
  taxRegime?: string;
  isActive?: boolean;
  notes?: string;
}

export interface LinkAccountInput {
  sellerAccountId: string;
  notes?: string;
  replaceActiveLink: boolean;
}

export const TAX_REGIMES = [
  { value: 'SIMPLES_NACIONAL', label: 'Simples Nacional' },
  { value: 'LUCRO_PRESUMIDO', label: 'Lucro Presumido' },
  { value: 'LUCRO_REAL', label: 'Lucro Real' },
  { value: 'MEI', label: 'Microempreendedor Individual (MEI)' },
  { value: 'OUTRO', label: 'Outro / Não Informado' },
] as const;
