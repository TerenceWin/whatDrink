import requests
from bs4 import BeautifulSoup
import json

def scrape_drink_info(url: str) -> dict:
    headers = {"User-Agent": "Mozilla/5.0"}
    response = requests.get(url, headers=headers)
    response.encoding = 'utf-8' 
    soup = BeautifulSoup(response.text, "html.parser")
    text = soup.get_text(separator="\n", strip=True)
    return {"raw_text": text[:3000]}  # 先只取前3000字

# 测试
if __name__ == "__main__":
    url = input("输入饮料网页URL: ")
    result = scrape_drink_info(url)
    print(result["raw_text"])