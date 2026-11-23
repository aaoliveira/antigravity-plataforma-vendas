import { Page, expect } from '@playwright/test';
import AxeBuilder from '@axe-core/playwright';

export async function checkA11y(page: Page, contextName: string) {
  const accessibilityScanResults = await new AxeBuilder({ page })
    .withTags(['wcag2a', 'wcag2aa', 'wcag21aa'])
    .analyze();

  const criticalOrSeriousViolations = accessibilityScanResults.violations.filter(
    (v) => v.impact === 'critical' || v.impact === 'serious'
  );

  if (criticalOrSeriousViolations.length > 0) {
    console.error(`Violações A11y em ${contextName}:`, JSON.stringify(criticalOrSeriousViolations, null, 2));
  }

  expect(
    criticalOrSeriousViolations,
    `Violações críticas ou sérias de acessibilidade WCAG detectadas em ${contextName}`
  ).toEqual([]);
}
