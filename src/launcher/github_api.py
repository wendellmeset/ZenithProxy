from requests import Session
from requests.adapters import HTTPAdapter
from urllib3 import Retry

class GitHubAPI:
    launch_config = None

    def __init__(self, launch_config):
        self.launch_config = launch_config

    def get_base_url(self):
        if self.launch_config.repo_owner == "rfresh2" and self.launch_config.repo_name == "ZenithProxy":
            host = "github.2b2t.vc"
        else:
            host = "api.github.com"
        return f"https://{host}/repos/{self.launch_config.repo_owner}/{self.launch_config.repo_name}/releases"

    def get_headers(self):
        return {
            "User-Agent": "ZenithProxy/" + self.launch_config.local_version,
            "Accept": "application/vnd.github+json",
            "X-GitHub-Api-Version": "2022-11-28",
            "Connection": "close",
        }

    def _create_session(self):
        retries = Retry(
            total=1,
            connect=1,
            read=1,
            status=1,
            redirect=5,
            backoff_factor=1,
            status_forcelist=[500, 502, 503, 504]
        )
        session = Session()
        adapter = HTTPAdapter(max_retries=retries)
        session.mount("https://", adapter)
        return session

    def _send_request(self, url, headers, params=None, timeout=10, allow_redirects=False):
        try:
            with self._create_session() as session:
                response = session.get(url, headers=headers, params=params, timeout=timeout, allow_redirects=allow_redirects)
                if response.status_code == 200:
                    return response
                raise Exception(f"Request to {url} failed with status code {response.status_code}")
        except Exception as e:
            raise Exception(f"Request to {url} failed with exception {e}")

    def get_latest_release_and_ver(self, channel):
        try:
            response = self._send_request(self.get_base_url(), self.get_headers(), params={"per_page": 100})
            releases = response.json()
            latest_release = max(
                (r for r in releases if not r["draft"] and r["tag_name"].endswith("+" + channel)),
                key=lambda r: r["published_at"],
                default=None,
            )
            return (latest_release["id"], latest_release["tag_name"]) if latest_release else None
        except Exception as e:
            print("Failed to get releases:", e)
        return None

    def get_release_for_ver(self, tag_name):
        url = f"{self.get_base_url()}/tags/{tag_name}"
        try:
            response = self._send_request(url, self.get_headers())
            release = response.json()
            return release["id"], release["tag_name"]
        except Exception as e:
            print("Failed to get release for version:", e)
        return None

    def get_asset_id(self, release_id, asset_name, tag=False):
        url = f"{self.get_base_url()}/{'tags/' if tag else ''}{release_id}"
        try:
            response = self._send_request(url, self.get_headers())
            return next((asset["id"] for asset in response.json()["assets"] if asset["name"] == asset_name), None)
        except Exception as e:
            print("Failed to get release asset ID:", e)
        return None

    def get_release_asset_id(self, release_id, asset_name):
        return self.get_asset_id(release_id, asset_name)

    def get_release_tag_asset_id(self, release_id, asset_name):
        return self.get_asset_id(release_id, asset_name, True)

    def download_asset(self, asset_id):
        url = f"{self.get_base_url()}/assets/{asset_id}"
        download_headers = self.get_headers()
        download_headers["Accept"] = "application/octet-stream"
        try:
            response = self._send_request(url, download_headers, allow_redirects=True, timeout=60)
            return response.content
        except Exception as e:
            print("Failed to download asset:", e)
            return None
