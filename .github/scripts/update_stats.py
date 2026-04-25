import os
import re
import sys
import json
import urllib.request
import urllib.error
from datetime import datetime, timezone

OWNER = "Heldyy90"
REPO = "TextManager"

README_PATH = "README.md"
STATS_PATH = "STATS.md"

START = "<!-- TEXTMANAGER-STATS:START -->"
END = "<!-- TEXTMANAGER-STATS:END -->"

API = f"https://api.github.com/repos/{OWNER}/{REPO}"


def github_get(url: str):
    token = os.environ.get("GITHUB_TOKEN", "")

    headers = {
        "Accept": "application/vnd.github+json",
        "User-Agent": "TextManager-Stats-Updater",
    }

    if token:
        headers["Authorization"] = f"Bearer {token}"

    request = urllib.request.Request(url, headers=headers)

    with urllib.request.urlopen(request, timeout=30) as response:
        return json.loads(response.read().decode("utf-8"))


def load_releases():
    releases = []
    page = 1

    while True:
        url = f"{API}/releases?per_page=100&page={page}"
        data = github_get(url)

        if not data:
            break

        releases.extend(data)
        page += 1

    return [release for release in releases if not release.get("draft")]


def format_number(number: int) -> str:
    return f"{number:,}".replace(",", " ")


def safe_date(value: str) -> str:
    if not value:
        return "—"

    try:
        return datetime.fromisoformat(value.replace("Z", "+00:00")).strftime("%Y-%m-%d")
    except Exception:
        return value[:10]


def release_downloads(release):
    assets = release.get("assets", [])

    all_downloads = sum(int(asset.get("download_count", 0)) for asset in assets)

    jar_assets = [
        asset for asset in assets
        if asset.get("name", "").lower().endswith(".jar")
    ]

    jar_downloads = sum(int(asset.get("download_count", 0)) for asset in jar_assets)

    return all_downloads, jar_downloads, jar_assets


def build_stats(releases):
    releases = sorted(
        releases,
        key=lambda release: release.get("published_at") or "",
        reverse=True
    )

    total_all_downloads = 0
    total_jar_downloads = 0
    total_assets = 0

    rows = []

    for release in releases:
        all_downloads, jar_downloads, jar_assets = release_downloads(release)

        total_all_downloads += all_downloads
        total_jar_downloads += jar_downloads
        total_assets += len(release.get("assets", []))

        rows.append({
            "tag": release.get("tag_name", "—"),
            "name": release.get("name") or release.get("tag_name", "—"),
            "date": safe_date(release.get("published_at", "")),
            "url": release.get("html_url", ""),
            "all_downloads": all_downloads,
            "jar_downloads": jar_downloads,
            "assets_count": len(release.get("assets", [])),
            "jar_names": [asset.get("name", "") for asset in jar_assets],
        })

    updated = datetime.now(timezone.utc).strftime("%Y-%m-%d %H:%M UTC")

    if rows:
        latest = rows[0]
        latest_version = f"[`{latest['tag']}`]({latest['url']})"
        latest_date = latest["date"]
    else:
        latest_version = "—"
        latest_date = "—"

    readme_block = f"""{START}
## 📊 Статистика проекта

| Показатель | Значение |
|---|---:|
| 📥 Всего скачиваний файлов | **{format_number(total_all_downloads)}** |
| 🧩 Скачиваний `.jar` модов | **{format_number(total_jar_downloads)}** |
| 🚀 Всего релизов | **{format_number(len(rows))}** |
| 📦 Всего файлов в релизах | **{format_number(total_assets)}** |
| 🆕 Последняя версия | **{latest_version}** |
| 📅 Дата последнего релиза | **{latest_date}** |

[➡️ Подробная статистика по всем релизам](./STATS.md)

_Автообновление: {updated}_
{END}"""

    stats_lines = []

    stats_lines.append("# 📊 TextManager — статистика релизов")
    stats_lines.append("")
    stats_lines.append(f"_Последнее обновление: **{updated}**_")
    stats_lines.append("")
    stats_lines.append("## Общая статистика")
    stats_lines.append("")
    stats_lines.append("| Показатель | Значение |")
    stats_lines.append("|---|---:|")
    stats_lines.append(f"| 📥 Всего скачиваний файлов | **{format_number(total_all_downloads)}** |")
    stats_lines.append(f"| 🧩 Скачиваний `.jar` модов | **{format_number(total_jar_downloads)}** |")
    stats_lines.append(f"| 🚀 Всего релизов | **{format_number(len(rows))}** |")
    stats_lines.append(f"| 📦 Всего файлов в релизах | **{format_number(total_assets)}** |")
    stats_lines.append("")
    stats_lines.append("## Релизы")
    stats_lines.append("")
    stats_lines.append("| Версия | Дата | `.jar` скачивания | Все скачивания | Файлов |")
    stats_lines.append("|---|---:|---:|---:|---:|")

    if rows:
        for row in rows:
            stats_lines.append(
                f"| [`{row['tag']}`]({row['url']}) | {row['date']} | "
                f"{format_number(row['jar_downloads'])} | "
                f"{format_number(row['all_downloads'])} | "
                f"{format_number(row['assets_count'])} |"
            )
    else:
        stats_lines.append("| — | — | 0 | 0 | 0 |")

    stats_lines.append("")
    stats_lines.append("## Файлы `.jar` по релизам")
    stats_lines.append("")

    if rows:
        for row in rows:
            stats_lines.append(f"### {row['tag']}")

            if row["jar_names"]:
                for jar_name in row["jar_names"]:
                    stats_lines.append(f"- `{jar_name}`")
            else:
                stats_lines.append("- `.jar` файл не найден")

            stats_lines.append("")
    else:
        stats_lines.append("Релизов пока нет.")
        stats_lines.append("")

    return readme_block, "\n".join(stats_lines).rstrip() + "\n"


def replace_block(text: str, block: str) -> str:
    pattern = re.compile(
        re.escape(START) + r".*?" + re.escape(END),
        re.DOTALL
    )

    if pattern.search(text):
        return pattern.sub(block, text)

    marker = "\n\n## 📸 Скриншоты"

    if marker in text:
        return text.replace(marker, "\n\n" + block + "\n\n---" + marker, 1)

    return text.rstrip() + "\n\n" + block + "\n"


def main():
    try:
        releases = load_releases()

        readme_block, stats_md = build_stats(releases)

        with open(README_PATH, "r", encoding="utf-8") as file:
            readme = file.read()

        updated_readme = replace_block(readme, readme_block)

        with open(README_PATH, "w", encoding="utf-8", newline="\n") as file:
            file.write(updated_readme)

        with open(STATS_PATH, "w", encoding="utf-8", newline="\n") as file:
            file.write(stats_md)

        print("TextManager stats updated successfully.")

    except urllib.error.HTTPError as error:
        print(f"GitHub API error: {error.code} {error.reason}", file=sys.stderr)
        print(error.read().decode("utf-8", errors="replace"), file=sys.stderr)
        sys.exit(1)


if __name__ == "__main__":
    main()
