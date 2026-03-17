# Page Standardization Checklist

## Page Types

### Settings
- Template: `SettingsMaterialScreen`
- Use for grouped settings, switches, preference-like navigation, and destructive settings rows.
- Current standardized pages:
  - `My`
  - `Theme`
  - `Backup`
  - `Other`
  - `About`

### Search Management
- Template: `LegadoSearchAppBar` + list/grid content on `background`
- Use for searchable management screens with top-level filters and bulk actions.
- Current standardized pages:
  - `BookSource`
  - `RssSource`
- Needs follow-up:
  - `Explore`
  - `BookshelfManage`
  - `BookSourceDebug`

### Rule / Edit List
- Template: `RuleManageMaterialScreen` or `LegadoSmallAppBar` + `LegadoSectionCard`
- Use for rules, selectable lists, and batch operations.
- Current standardized pages:
  - `ReplaceRule`
  - `DictRule`
  - `TxtTocRule`
  - `Toc`

### Detail / Summary
- Template: `LegadoPageHeader` + `LegadoSectionCard` + `LegadoListRow`
- Use for summary screens, detail dashboards, and read-only grouped information.
- Current standardized pages:
  - `ReadRecord`
  - `FileManage`
  - `AllBookmark`
  - `Rss`
  - `Bookshelf` top-level chrome

## Migration Status

### Standardized
- `MainActivity`
- `MyFragment`
- `ThemeComposeActivity`
- `BackupComposeActivity`
- `OtherComposeActivity`
- `AboutActivity`
- `ReadRecordActivity`
- `FileManageActivity`
- `AllBookmarkActivity`
- `BookSourceActivity`
- `RssSourceActivity`
- `ReplaceRuleActivity`
- `DictRuleActivity`
- `TxtTocRuleActivity`
- `TocActivity`

### Ready For Template Migration
- `ExploreFragment`
- `BookshelfManageActivity`
- `SearchContentActivity`
- `BookSourceDebugActivity`
- `RssFavoritesActivity`

### Needs Template Capability First
- `ReadRssActivity`
- `SourceLoginDialog`
- `FilePickerDialog`
- `ChangeChapterSourceDialog`

### Deferred
- Reader pages and reader configuration dialogs
- Complex editing dialogs and bottom sheets
- XML-first pages that only need token unification for now
