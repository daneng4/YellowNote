from wave import WAVE_FORMAT_PCM
from scipy.io import wavfile
from scipy import signal
import numpy as np
from matplotlib import pyplot as plt
import os
import io
import soundfile as sf
from scipy.io.wavfile import write

# Scipy = 사이파이, 과학기술계산을 위한 python 라이브러리 
# signal.firwin(차수, 차단주파수, 필터)

def highpassfilter():
    nowdir = os.getcwd()
    # 음성 + 펜 파형 
    wav = "C:\\vscode\\beforeHpf.wav"
    #file_dir, file_id) = os.path.split(wav)
    print("전달완료")
    #x = np.frombuffer(wav, dtype=np.int16)
    sr , x = wavfile.read(wav)

    # high pass filter
    b = signal.firwin(101, cutoff=500, fs = sr, pass_zero='highpass')
    x2 = signal.lfilter(b, [1.0], x)

    wav_hpf = nowdir+"\\"+"AfterHpf.wav"
    wavfile.write(wav_hpf, sr, x2.astype(np.int16))
    # return wavfile.write(wav_hpf, sr, x2.astype(np.int16))
    # # wavfile.write(wav_hpf, sr, x2)
    # # ### 여기까지 파일생성을 위한 필수코드


