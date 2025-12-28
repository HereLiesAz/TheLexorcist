# Palette's Journal

## 2024-05-22 - Preventing "Peek-a-boo" Buttons
**Learning:** Conditional rendering of primary action buttons based on form validity creates jarring layout shifts and confuses users ("Where did the save button go?").
**Action:** Always keep the primary action button visible. If the form is invalid, handle the click by showing an inline error message and highlighting the invalid field. This guides the user instead of punishing them.

## 2024-05-22 - Icon-Only Buttons
**Learning:** Using text labels for common actions like "Delete" or "Close" in tight spaces (like list items) can be cluttered and less intuitive than standard icons.
**Action:** Use standard `IconButton`s with Material Icons for common actions, ensuring they have proper `contentDescription`s for accessibility. This improves visual scanability and adheres to platform standards.
